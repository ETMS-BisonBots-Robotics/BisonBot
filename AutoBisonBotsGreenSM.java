package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;

@Autonomous(name = "AutoBisonBots",group = "21397")
//@Disabled
public class AutoBisonBotsGreenSM extends OpMode {
    public static final int FORWARD_AMOUNT = 1000;
    public static final double POWER_LEVEL = 1 ;
    public static final int SLEEP_SECONDS = 2;
    public static final int SLIDE_AMOUNT = 1500;

    private DcMotor leftFrontMotor,rightFrontMotor,leftBackMotor,rightBackMotor;
    private double startingSleepTime;
    private boolean isDoneSeeping;
    private ElapsedTime runTime = new ElapsedTime();
    // private int leftFrontStaringPoss,rightFrontStaringPoss,leftBackStartingPoss,rightBackStartingPoss;
    // private int leftFrontTargetPoss,rightFrontTargetPoss,leftBackTargetPoss,rightBackTargetPoss;
    enum MachineStates{
        STOPPED,
        INIT,
        MOVE_FORWARD,
        SLEEP,
        END
    }
    private MachineStates currentState;
    private MachineStates previousState;
    private boolean isFirstTimeInThisState;

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
        leftBackMotor.setDirection(DcMotor.Direction.REVERSE);


        telemetry.addData("status","initalize");
    }

    @Override
    public void loop() {
        isFirstTimeInThisState = (currentState != previousState);
        previousState = currentState;

        switch (currentState){
            case INIT:
                currentState = MachineStates.MOVE_FORWARD;
                break;
            case SLEEP:
                if (isFirstTimeInThisState){
                    startingSleepTime = runTime.time();
                }
                isDoneSeeping = ((runTime.time() - startingSleepTime)> SLEEP_SECONDS);
                if (isDoneSeeping){
                    currentState = MachineStates.MOVE_FORWARD;
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
            case END:
                leftFrontMotor.setPower(0);
                rightFrontMotor.setPower(0);
                leftBackMotor.setPower(0);
                rightBackMotor.setPower(0);
                break;
        }
        telemetry.addData("states","current state:" + currentState);
        telemetry.addData("states","current state:" + previousState);
        telemetry.addData("motors","leftFront:" + leftFrontMotor.getCurrentPosition());
        telemetry.addData("motors","rightFront:" + rightFrontMotor.getCurrentPosition());
        telemetry.addData("motors","leftback:" + leftBackMotor.getCurrentPosition());
        telemetry.addData("motors","rightback:" + rightBackMotor.getCurrentPosition());

    }
}
//Gummy bear dvd album coming out on Nov. 13th with music, videos and extras!