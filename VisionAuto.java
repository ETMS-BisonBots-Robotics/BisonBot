package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;

import java.util.List;

/**
 * This 2022-2023 OpMode illustrates the basics of using the TensorFlow Object Detection API to
 * determine which image is being presented to the robot.
 *
 * Use Android Studio to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this opmode to the Driver Station OpMode list.
 *
 * IMPORTANT: In order to use this OpMode, you need to obtain your own Vuforia license key as
 * is explained below.
 */
@Autonomous(name = "VisionAuto",group = "21387")
//@Disabled
public class VisionAuto extends OpMode {
    public static final int FORWARD_AMOUNT = 1000;
    public static final double POWER_LEVEL = .5;
    public static final int SLIDE_LEFT_AMOUNT = 1200;
    public static final int SLIDE_RIGHT_AMOUNT = 1200;

    enum MachineStates {
        STOPPED,
        INIT,
        TENSOR_FLOW,
        MOVE_FORWARD,
        SLIDE_LEFT,
        SLIDE_RIGHT,
        SLEEP,
        END
    }
    private MachineStates currentState;
    private MachineStates previousState;
    private boolean isFirstTimeInThisState;

    enum ParkingSpots {
        PARKING_SPOT_A,
        PARKING_SPOT_B,
        PARKING_SPOT_C,
        UNKNOWN
    }
    ParkingSpots parkingSpot = ParkingSpots.UNKNOWN;

    private DcMotor leftFrontMotor,rightFrontMotor,leftBackMotor,rightBackMotor;

    private double startingSleepTime;
    private boolean isDoneSeeping;
    private ElapsedTime runTime = new ElapsedTime();
    public static final int SLEEP_SECONDS = 1;

    /*
     * Specify the source for the Tensor Flow Model.
     * If the TensorFlowLite object model is included in the Robot Controller App as an "asset",
     * the OpMode must to load it using loadModelFromAsset().  However, if a team generated model
     * has been downloaded to the Robot Controller's SD FLASH memory, it must to be loaded using loadModelFromFile()
     * Here we assume it's an Asset.    Also see method initTfod() below .
     */
    private static final String TFOD_MODEL_ASSET = "PowerPlay.tflite";
    // private static final String TFOD_MODEL_FILE  = "/sdcard/FIRST/tflitemodels/CustomTeamModel.tflite";


    private static final String[] LABELS = {
            "1 Bolt",
            "2 Bulb",
            "3 Panel"
    };

    /*
     * IMPORTANT: You need to obtain your own license key to use Vuforia. The string below with which
     * 'parameters.vuforiaLicenseKey' is initialized is for illustration only, and will not function.
     * A Vuforia 'Development' license key, can be obtained free of charge from the Vuforia developer
     * web site at https://developer.vuforia.com/license-manager.
     *
     * Vuforia license keys are always 380 characters long, and look as if they contain mos tly
     * random data. As an example, here is a example of a fragment of a valid key:
     *      ... yIgIzTqZ4mWjk9wd3cZO9T1axEqzuhxoGlfOOI2dRzKS4T0hQ8kT ...
     * Once you've obtained a license key, copy the string from the Vuforia web site
     * and paste it in to your code on the next line, between the double quotes.
     */
    private static final String VUFORIA_KEY = ("ASPzPfP/////AAABmf00+xsRW0JRooeGF/5SwJ5KNEbQFjv3TuTYOBnKFFTygemxv+Gz6btBaxbb7IyMgL4GUISLbbL5M2IBPlx4tQx8RsnOXR1ljatQZCECy67ErTFMPWJiBD1DhtwlXyxZNWm+rw261aRoLtzA2DTOR6n0e/ZQkx91JnbOSBDuYW1X9AEExPdbAlsI6KrRpcL6G2pHXYqBCBQKOoWhFEBBZ6/HdKBPKAibWykSgzk0q5g21uyL+nGqkVOXja/5RY2guJgpubfeyPtk1qv5TOHg19gKbfqShCwrCIw7CE6l9VKlwyvk5OIoQ2bin3JJFR/NA3OSnrf9WpcqTlqXUNgtbqoCKqQ2cTCfoKkVqlhxNF0i");

    /**
     * {@link #vuforia} is the variable we will use to store our instance of the Vuforia
     * localization engine.
     */
    private VuforiaLocalizer vuforia;

    /**
     * {@link #tfod} is the variable we will use to store our instance of the TensorFlow Object
     * Detection engine.
     */
    private TFObjectDetector tfod;

    /**
     * Initialize the Vuforia localization engine.
     */
    private void initVuforia() {
        /*
         * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
         */
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraName = hardwareMap.get(WebcamName.class, "Webcam 1");

        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);
    }

    /**
     * Initialize the TensorFlow Object Detection engine.
     */
    private void initTfod() {
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfodParameters.minResultConfidence = 0.80f;
        tfodParameters.isModelTensorFlow2 = true;
        tfodParameters.inputSize = 300;
        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);

        // Use loadModelFromAsset() if the TF Model is built in as an asset by Android Studio
        // Use loadModelFromFile() if you have downloaded a custom team model to the Robot Controller's FLASH.
        tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABELS);
        // tfod.loadModelFromFile(TFOD_MODEL_FILE, LABELS);
    }

    @Override
    public void init() {
        currentState = MachineStates.INIT;
        previousState = MachineStates.STOPPED;

        leftFrontMotor = hardwareMap.dcMotor.get("leftfront");
        rightFrontMotor = hardwareMap.dcMotor.get("rightfront");
        leftBackMotor = hardwareMap.dcMotor.get("leftback");
        rightBackMotor = hardwareMap.dcMotor.get("rightback");


        leftFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        leftBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        leftFrontMotor.setDirection(DcMotor.Direction.REVERSE);
        rightFrontMotor.setDirection(DcMotor.Direction.REVERSE);
        leftBackMotor.setDirection(DcMotor.Direction.REVERSE);
        rightBackMotor.setDirection(DcMotor.Direction.FORWARD);
        // The TFObjectDetector uses the camera frames from the VuforiaLocalizer, so we create that
        // first.
        initVuforia();
        initTfod();

        /**
         * Activate TensorFlow Object Detection before we wait for the start command.
         * Do it here so that the Camera Stream window will have the TensorFlow annotations visible.
         **/
        if (tfod != null) {
            tfod.activate();

            // The TensorFlow software will scale the input images from the camera to a lower resolution.
            // This can result in lower detection accuracy at longer distances (> 55cm or 22").
            // If your target is at distance greater than 50 cm (20") you can increase the magnification value
            // to artificially zoom in to the center of image.  For best results, the "aspectRatio" argument
            // should be set to the value of the images used to create the TensorFlow Object Detection model
            // (typically 16/9).
            tfod.setZoom(1.60, 16.0/16.0);
        }

        telemetry.addData("status","initalized");
    }

    @Override
    public void loop() {
        isFirstTimeInThisState = (currentState != previousState);
        previousState = currentState;

        switch (currentState){
            case INIT:
                currentState = MachineStates.SLEEP;
                break;
            case SLEEP:
                if (isFirstTimeInThisState){
                    startingSleepTime = runTime.time();
                }
                isDoneSeeping = ((runTime.time() - startingSleepTime)> SLEEP_SECONDS);
                if (isDoneSeeping){
                    currentState = MachineStates.TENSOR_FLOW;
                }
                break;
            case TENSOR_FLOW:
                if (tfod != null) {
                    // getUpdatedRecognitions() will return null if no new information is available since
                    // the last time that call was made.
                    List<Recognition> updatedRecognitions = tfod.getUpdatedRecognitions();
                    if (updatedRecognitions != null) {
                        telemetry.addData("# Objects Detected", updatedRecognitions.size());

                        // step through the list of recognitions and display image position/size information for each one
                        // Note: "Image number" refers to the randomized image orientation/number
                        for (Recognition recognition : updatedRecognitions) {
                            double col = (recognition.getLeft() + recognition.getRight()) / 2 ;
                            double row = (recognition.getTop()  + recognition.getBottom()) / 2 ;
                            double width  = Math.abs(recognition.getRight() - recognition.getLeft()) ;
                            double height = Math.abs(recognition.getTop()  - recognition.getBottom()) ;

                            telemetry.addData(""," ");
                            telemetry.addData("Image", "%s (%.0f %% Conf.)", recognition.getLabel(), recognition.getConfidence() * 10 );
                            telemetry.addData("- Position (Row/Col)","%.0f / %.0f", row, col);
                            telemetry.addData("- Size (Width/Height)","%.0f / %.0f", width, height);

                            if (recognition.getLabel() == LABELS[0]){
                                parkingSpot = ParkingSpots.PARKING_SPOT_A;
                            } else if (recognition.getLabel() == LABELS[1]){
                                parkingSpot = ParkingSpots.PARKING_SPOT_B;
                            } else if (recognition.getLabel() == LABELS[2]) {
                                parkingSpot = ParkingSpots.PARKING_SPOT_C;
                            }
                            telemetry.addData("parking","Parking:" + parkingSpot);
                            if(parkingSpot == ParkingSpots.PARKING_SPOT_A)
                            {
                                currentState = MachineStates.SLIDE_LEFT;
                            }else if (parkingSpot == ParkingSpots.PARKING_SPOT_B)
                            {
                                currentState = MachineStates.MOVE_FORWARD;
                            }else if (parkingSpot == ParkingSpots.PARKING_SPOT_C)
                            {
                                currentState = MachineStates.SLIDE_RIGHT;
                            }
                        }
                        telemetry.update();
                    }
                }
                break;
            case MOVE_FORWARD:
                if (isFirstTimeInThisState){
                    leftFrontMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    rightFrontMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    leftBackMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    rightBackMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);


                    leftFrontMotor.setTargetPosition(FORWARD_AMOUNT);
                    rightFrontMotor.setTargetPosition(FORWARD_AMOUNT);
                    leftBackMotor.setTargetPosition(FORWARD_AMOUNT);
                    rightBackMotor.setTargetPosition(FORWARD_AMOUNT);

                    leftFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    rightFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    leftBackMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    rightBackMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                    leftFrontMotor.setPower(POWER_LEVEL);
                    rightFrontMotor.setPower(POWER_LEVEL);
                    rightBackMotor.setPower(POWER_LEVEL);
                    leftBackMotor.setPower(POWER_LEVEL);
                }
                if (!leftFrontMotor.isBusy() || !rightFrontMotor.isBusy() || !leftBackMotor.isBusy() || !rightBackMotor.isBusy()){
                    leftFrontMotor.setPower(0);
                    rightFrontMotor.setPower(0);
                    leftBackMotor.setPower(0);
                    rightBackMotor.setPower(0);

                    leftFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    rightFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    leftBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    rightBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

                    currentState = MachineStates.END;
                }
                break;
            case SLIDE_LEFT:
                if (isFirstTimeInThisState){
                    leftFrontMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    rightFrontMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    leftBackMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    rightBackMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

                    leftFrontMotor.setTargetPosition(-SLIDE_LEFT_AMOUNT);
                    rightFrontMotor.setTargetPosition(SLIDE_LEFT_AMOUNT);
                    leftBackMotor.setTargetPosition(SLIDE_LEFT_AMOUNT);
                    rightBackMotor.setTargetPosition(-SLIDE_LEFT_AMOUNT);

                    leftFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    rightFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    leftBackMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    rightBackMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                    leftFrontMotor.setPower(-POWER_LEVEL);
                    rightFrontMotor.setPower(POWER_LEVEL);
                    leftBackMotor.setPower(POWER_LEVEL);
                    rightBackMotor.setPower(-POWER_LEVEL);
                }
                if (!leftFrontMotor.isBusy() || !rightFrontMotor.isBusy() || !leftBackMotor.isBusy() || !rightBackMotor.isBusy()){
                    leftFrontMotor.setPower(0);
                    rightFrontMotor.setPower(0);
                    leftBackMotor.setPower(0);
                    rightBackMotor.setPower(0);

                    leftFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    rightFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    leftBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    rightBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

                    currentState = MachineStates.MOVE_FORWARD;
                }
                break;
            case SLIDE_RIGHT:
                if (isFirstTimeInThisState){
                    leftFrontMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    rightFrontMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    leftBackMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    rightBackMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

                    leftFrontMotor.setTargetPosition(SLIDE_LEFT_AMOUNT);
                    rightFrontMotor.setTargetPosition(-SLIDE_LEFT_AMOUNT);
                    leftBackMotor.setTargetPosition(-SLIDE_LEFT_AMOUNT);
                    rightBackMotor.setTargetPosition(SLIDE_LEFT_AMOUNT);

                    leftFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    rightFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    leftBackMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    rightBackMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

                    leftFrontMotor.setPower(POWER_LEVEL);
                    rightFrontMotor.setPower(-POWER_LEVEL);
                    leftBackMotor.setPower(-POWER_LEVEL);
                    rightBackMotor.setPower(POWER_LEVEL);
                }
                if (!leftFrontMotor.isBusy() || !rightFrontMotor.isBusy() || !leftBackMotor.isBusy() || !rightBackMotor.isBusy()){
                    leftFrontMotor.setPower(0);
                    rightFrontMotor.setPower(0);
                    leftBackMotor.setPower(0);
                    rightBackMotor.setPower(0);

                    leftFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    rightFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    leftBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    rightBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

                    currentState = MachineStates.MOVE_FORWARD;
                }
                break;
            case END:
                leftFrontMotor.setPower(0);
                rightFrontMotor.setPower(0);
                leftBackMotor.setPower(0);
                rightBackMotor.setPower(0);
                break;
        }
        telemetry.addData("states","current state:" + currentState);
        telemetry.addData("parking","Parking:" + parkingSpot);
        telemetry.addData("motors","leftFront:" + leftFrontMotor.getCurrentPosition());
        telemetry.addData("motors","rightFront:" + rightFrontMotor.getCurrentPosition());
        telemetry.addData("motors","leftback:" + leftBackMotor.getCurrentPosition());
        telemetry.addData("motors","rightback:" + rightBackMotor.getCurrentPosition());
    }
}
