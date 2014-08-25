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
		if(cnt > 50) {		//Control the send freq
			String Msg = String.format("%d,%d,%d,%d", grab, base, vertical, frontAndBack);
			//String Msg = String.format("%d,%d,%d,%d", 0, base, 0, 0);
			mRS232.write(Msg);
			//mRS232.read();
			cnt = 0;
		}
	}
	
	/**
	 * Move the robot arm
	 * @param mov
	 * @param pos
	 * @param grab Grab control£¬Range: 5 ~ 55
	 */
	public void moveArm(Vector mov, Vector pos, int grab) {
		int threshold = 0;
		
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
			x = -x;		//The motor control signal is reverse
			
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
			z = -z;		//The motor control signal is reverse
			
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
	        System.out.println("Initialized");
	    }

	    public void onConnect(Controller controller) {
	        System.out.println("Connected");
	    }

	    public void onDisconnect(Controller controller) {
	        //Note: not dispatched when running in a debugger.
	        System.out.println("Disconnected");
	    }

	    public void onExit(Controller controller) {
	        System.out.println("Exited");
	    }

	    public void onFrame(Controller controller) {
	        // Get the most recent frame and report some basic information
	        Frame frame = controller.frame();

	        //Get hands
	        for(Hand hand : frame.hands()) {
	        	//System.out.println("v : " + hand.palmVelocity() + ", pos : " + hand.palmPosition() + ", grab : " + hand.grabStrength());
	        	int grab = 0;
	            grab = ((int)(hand.grabStrength() * 100.0) / 2) + 5;		//0 ~ 1.0×ª»»Îª 5 ~ 55
	            
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
		System.out.println("Please input COM port :");
		Scanner sc = new Scanner(System.in);
		int RS232_Port = sc.nextInt();
		//int RS232_Port = 33;		//You should modify to your own RS232 port on your computer
		
		MotionMeArm leap = new MotionMeArm();
		leap.init(RS232_Port);
        
        System.out.println("Press Enter to quit...");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        leap.destroy();
        sc.close();
	}

}
