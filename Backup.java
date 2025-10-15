import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Backup implements BackupInterface {

    private GameState latestState = new GameState();

    /**
     * This method is now called via RMI from the Primary server.
     * Its output will correctly appear in this Backup server's terminal.
     */
    @Override
    public void updateState(GameState newState) throws RemoteException {
        this.latestState = newState;
        System.out.println("========================================");
        System.out.println("✅ REPLICATION: State update received!");
        System.out.println("   Status: " + latestState.statusMessage);
        System.out.println("   Player Hand: " + latestState.playerHand);
        System.out.println("========================================");
    }

    public static void main(String[] args) {
        try {
            Backup backup = new Backup();
            BackupInterface stub = (BackupInterface) UnicastRemoteObject.exportObject(backup, 0);

            Registry registry = LocateRegistry.getRegistry("localhost");
            registry.bind("BlackjackBackup", stub); // Bind with a unique name

            System.out.println("Backup Server is ready and waiting for updates.");
            backup.startHeartbeatMonitor();
        } catch (Exception e) {
            System.err.println("Backup Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startHeartbeatMonitor() {
        new Thread(() -> {
            int failures = 0;
            final int FAILURE_THRESHOLD = 3;
            while (true) {
                try {
                    Thread.sleep(3000);
                    Registry registry = LocateRegistry.getRegistry("localhost");
                    GameInterface primary = (GameInterface) registry.lookup("BlackjackPrimary");
                    primary.ping();
                    failures = 0;
                    System.out.println("Heartbeat: Primary is alive.");
                } catch (Exception e) {
                    failures++;
                    System.err.println("Heartbeat: Primary seems down! Failure count: " + failures);
                    if (failures >= FAILURE_THRESHOLD) {
                        System.out.println("🚨 Primary confirmed down. Promoting backup...");
                        promoteToPrimary();
                        break;
                    }
                }
            }
        }).start();
    }
    
    private void promoteToPrimary() {
        try {
            Server newPrimary = new Server(this.latestState);
            GameInterface stub = (GameInterface) UnicastRemoteObject.exportObject(newPrimary, 0);
            Registry registry = LocateRegistry.getRegistry("localhost");
            registry.rebind("BlackjackPrimary", stub);
            System.out.println("👑 Backup has been promoted and is now the Primary Server.");
        } catch (Exception e) {
            System.err.println("Error during promotion: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
