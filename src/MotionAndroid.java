/**
 * Control Android Phone Via Leap Motion
 * @author caisenchuan@163.com
 * */

import java.io.IOException;
import java.util.Scanner;

import com.leapmotion.leap.*;

/**
 * Control Android via Leap Motion
 * The work flow is simple:
 * 1. Leap motion catch the hand's movement
 * 2. Transfer the movement into mouse movement
 * 3. Send the commands to Arduino(Should support HID, like leonardo)
 * 4. Arduino transfer the commands to HID control cmd
 */
class MotionAndroid {
    private int cnt = 0;
    private long mLastClick = 0;
    private RS232 mRS232;
    private AndroidListener listener;
    private Controller controller;
    
    /**
     * initialize
     * @param port - RS232 Port on PC
     */
    public void init(int port) {
        mRS232 = new RS232();
        mRS232.open(port);
        
        listener = new AndroidListener(this);
        controller = new Controller();
        
        controller.addListener(listener);
    }
    
    /**
     * destory
     */
    public void destory() {
        controller.removeListener(listener);
        
        mRS232.close();
    }
    
    /**
     * Send cmd via RS232
     * The cmd format :
     * mouse left move, mouse right move, mouse up move, mouse down move, mouse click control
     * Example:
     * 20, 10, 20, 30, 1
     */
    public void sendToRS232(int left, int right, int up, int down, int click, boolean force) {
        cnt++;
        if(cnt > 4 || force) {        //Control the send freq
            String Msg = String.format("%d,%d,%d,%d,%d", up, down, right, left, click);
            mRS232.write(Msg);
            cnt = 0;
        }
    }
    
    /**
     * Send Mouse movement
     * @param v
     * @param pos
     * @param grab
     */
    public void sendMouseMove(Vector v, Vector pos, boolean grab) {
        
        float x = v.getX();
        float y = v.getY();
        
        if(Math.abs(x) > 100 || Math.abs(y) > 100) {
            int left = 0;
            int right = 0;
            int up = 0;
            int down = 0;
            int rate = 6;
            
            if(x > 100) {
                left = (int)x / rate;
            } else if(x < -100) {
                right = (int)Math.abs(x) / rate;
            } else {
                //Don't move
            }
            
            if(y > 100) {
                up = (int)y / rate;
            } else if(y < -100){
                down = (int)Math.abs(y) / rate;
            } else {
                //Don't move
            }
            
            int click = 0;
            if(grab) {
                click = 1;
            }
            
            sendToRS232(left, right, up, down, click, false);
        }
    }
    
    /**
     * Send Click
     */
    public void sendMouseClick() {
        mLastClick = System.currentTimeMillis();
        sendToRS232(0, 0, 0, 0, 1, true);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendToRS232(0, 0, 0, 0, 0, true);
    }

    /**
     * Leap Motion Listener
     */
    static private class AndroidListener extends Listener {
        private MotionAndroid mLeapAndroid;
        
        public AndroidListener(MotionAndroid android) {
            mLeapAndroid = android;
        }
        
        public void onInit(Controller controller) {
            System.out.println("Initialized");
        }

        public void onConnect(Controller controller) {
            System.out.println("Connected");
            controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
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
                if(isTap(frame)) {
                    //Click
                    mLeapAndroid.sendMouseClick();
                } else {
                    //Send hand position
                    boolean grab = false;
                    if(hand.grabStrength() > 0.5) {
                        grab = true;
                    } else {
                        grab = false;
                    }
                    mLeapAndroid.sendMouseMove(hand.palmVelocity(), hand.palmPosition(), grab);
                }
                
                //We use just one hand
                break;
            }
        }
        
        /**
         * Recognize tap gesture
         * @param frame
         * @return
         */
        private boolean isTap(Frame frame) {
            boolean ret = false;
            boolean hasClick = false;
            
            GestureList gestures = frame.gestures();
            for (int i = 0; i < gestures.count(); i++) {
                Gesture gesture = gestures.get(i);
                switch (gesture.type()) {
                    case TYPE_KEY_TAP:
                        KeyTapGesture keyTap = new KeyTapGesture(gesture);
                        System.out.println("  Key Tap id: " + keyTap.id()
                                   + ", " + keyTap.state()
                                   + ", position: " + keyTap.position()
                                   + ", direction: " + keyTap.direction());
                        hasClick = true;
                        break;
                    default:
                        System.out.println("Unknown gesture type.");
                        break;
                }
            }
            
            long currTime = System.currentTimeMillis();
            if(hasClick && (currTime - mLeapAndroid.mLastClick > 500)) {
                //Avoid repeat click cmd in short time
                ret = true;
            }
            
            return ret;
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
        
        MotionAndroid leap = new MotionAndroid();
        leap.init(RS232_Port);
        
        System.out.println("Press Enter to quit...");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        leap.destory();
        sc.close();
    }
}
