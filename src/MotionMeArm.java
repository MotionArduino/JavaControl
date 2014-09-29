/**
 * Control MeArm via Leap Motion
 * @author caisenchuan@163.com
 * */

import java.io.IOException;
import java.util.Scanner;

import com.leapmotion.leap.*;

/**
 * Control MeArm via Leap Motion
 * The work flow is simple:
 * 1. Leap motion catch the hand's movement
 * 2. Transfer the movement into moto control commands
 * 3. Send the commands to MeArm via RS232
 */
public class MotionMeArm {
    private static final String TAG = MotionMeArm.class.getSimpleName();
    
    private static final int MOVE_INTERVAL = 20;
    
    private int cnt = 0;
    private RS232 mRS232 = null;
    private ArmListener listener;
    private Controller controller;
    
    /**
     * initialize
     * @param port
     */
    public void init(int port) {
        mRS232 = new RS232();
        mRS232.open(port);
        
        listener = new ArmListener(this);
        controller = new Controller();
        
        controller.addListener(listener);
    }
    
    /**
     * destroy
     */
    public void destroy() {
        controller.removeListener(listener);

        mRS232.close();
    }
    
    /**
     * Send cmd via RS232
     * The cmd format :
     * grab control, Base Move control, Vertical Move control, Front and back Move control
     * Example:
     * 20, 10, 20, 30
     */
    public void sendToRS232(int grab, int base, int vertical, int frontAndBack) {
        cnt++;
        if(cnt > MOVE_INTERVAL) {        //Control the send freq
            String Msg = String.format("%d,%d,%d,%d", grab, base, vertical, frontAndBack);
            mRS232.write(Msg);
            cnt = 0;
        }
    }
    
    /**
     * Move the robot arm
     * @param mov
     * @param pos
     * @param grab Grab control��Range: 5 ~ 55
     */
    public void moveArm(Vector mov, Vector pos, int grab) {
        int threshold = 0;
        
        KLog.d(TAG, "mov : %s, pos : %s", mov, pos);
        
        //Transfer via Leap Motion coordinate to robot arm cmd
        //Robot move angle
        //Base: 0 ~ 180
        //Vertical Move: 30 ~ 120
        //Front and back Move: 0 ~ 60
        
        //Leap Motion coordinate:
        //x: -90 ~ 90
        //y: 80 ~ 170
        //z: -30 ~ 30
        
        if(Math.abs(mov.getX()) > threshold || 
           Math.abs(mov.getY()) > threshold || 
           Math.abs(mov.getZ()) > threshold) {
            //Step.1 Limit the range
            int x = (int)pos.getX();
            //x
            if(x > 90) {
                x = 90;
            } else if(x < -90) {
                x = -90;
            } else {
                //...
            }
            x = -x;        //The motor control signal is reverse
            
            //y
            int y = (int)pos.getY();
            if(y > 170) {
                y = 170;
            } else if(y < 80) {
                y = 80;
            } else {
                //...
            }
            
            //z
            int z = (int)pos.getZ();
            if(z > 30) {
                z = 30;
            } else if(z < -30) {
                z = -30;
            } else {
                //...
            }
            z = -z;        //The motor control signal is reverse
            
            //Step.2 Coordinate transfer
            x = x + 90;
            y = y - 50;
            z = z + 30;
            
            //Step.3 Send the cmd
            sendToRS232(grab, x, y, z);
        }
    }

    /**
     * Leap motion listener
     */
    private static class ArmListener extends Listener {
		private MotionMeArm mLeapArm;
        
        public ArmListener(MotionMeArm arm) {
            mLeapArm = arm;
        }
        
        public void onInit(Controller controller) {
            KLog.d(TAG, "Initialized");
        }

        public void onConnect(Controller controller) {
        	KLog.d(TAG, "Connected");
        }

        public void onDisconnect(Controller controller) {
        	KLog.d(TAG, "Disconnected");
        }

        public void onExit(Controller controller) {
        	KLog.d(TAG, "Exited");
        }

        public void onFrame(Controller controller) {
            // Get the most recent frame and report some basic information
            Frame frame = controller.frame();

            //Get hands
            for(Hand hand : frame.hands()) {
                int grab = 0;
                grab = ((int)(hand.grabStrength() * 100.0) / 2) + 5;        //0 ~ 1.0 transfer to: 5 ~ 55
                
                mLeapArm.moveArm(hand.palmVelocity(), hand.palmPosition(), grab);
                
                //We use just one hand
                break;
            }
        }
    }
    
    /**
     * Test function
     * @param args
     */
    public static void main(String[] args) {
    	KLog.d(TAG, "Please input COM port :");
        Scanner sc = new Scanner(System.in);
        int RS232_Port = sc.nextInt();
        
        MotionMeArm leap = new MotionMeArm();
        leap.init(RS232_Port);
        
        KLog.d(TAG, "Press Enter to quit...");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        leap.destroy();
        sc.close();
    }

}
