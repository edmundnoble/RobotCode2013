package frc.t4069.year2.robots.subsystems;

import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.Jaguar;
import edu.wpi.first.wpilibj.SpeedController;
import frc.t4069.year2.robots.Constants;
import frc.t4069.year2.utils.math.LowPassFilter;

/**
 * RobotDrive is obviously too complicated, amirite? This class includes a low
 * pass filter.
 * 
 * Recommended setup is tank drive based robot, with 2 or 4 Jaguars (not
 * victors). If 4 jags are used, use a PWM splitter to split the PWM signal.
 * Jaguars should be on brake mode.
 * 
 * Controller (Logitech Game Controller) should be mapped to trigger to be
 * forward/reverse, and left joystick's x to be left and right. These values are
 * passed to arcadeDrive as the move and turn value respectively.
 * 
 * @author Shuhao Wu, Edmund Noble
 */
public class DriveTrain {

	private static DriveTrain drivetrain;

	private SpeedController m_leftJaguar;
	private SpeedController m_rightJaguar;
	private LowPassFilter m_leftLP;
	private LowPassFilter m_rightLP;
	private Encoder m_leftEnc;
	private Encoder m_rightEnc;

	private double m_limit = 1.0;
	private double m_leftLimit = 1.0;
	private double m_rightLimit = 1.0;

	/**
	 * Initializes a new drive object with the RC value of 250.
	 */

	public static DriveTrain getDriveTrain() {
		return getDriveTrain(250);
	}

	public static DriveTrain getDriveTrain(double RC) {
		Encoder leftEncoder = new Encoder(Constants.LEFT_ENC_1,
				Constants.LEFT_ENC_2);
		Encoder rightEncoder = new Encoder(Constants.RIGHT_ENC_1,
				Constants.RIGHT_ENC_2);
		double distancePerPulse = Constants.PULSES_PER_REVOLUTION
				/ Constants.WHEEL_CIRCUMFERENCE;
		leftEncoder.setDistancePerPulse(distancePerPulse);
		rightEncoder.setDistancePerPulse(distancePerPulse);
		return getDriveTrain(new Jaguar(Constants.LEFT_MOTOR), new Jaguar(
				Constants.RIGHT_MOTOR), leftEncoder, rightEncoder, RC);
	}

	public static DriveTrain getDriveTrain(SpeedController leftJaguar,
			SpeedController rightJaguar, Encoder leftEncoder,
			Encoder rightEncoder, double RC) {
		return (drivetrain == null ? drivetrain = new DriveTrain(leftJaguar,
				rightJaguar, leftEncoder, rightEncoder, RC) : drivetrain);
	}

	/**
	 * Initializes a new drive object with a custom RC value.
	 * 
	 * @param RC
	 *            The RC value used for the drive train.
	 */

	/**
	 * Initializes a new drive train with all custom stuff. Recommend to use
	 * Jaguars as oppose to Victors. Victors didn't seem to like the Low Pass
	 * Filter as much...
	 * 
	 * If there's 4 motors, use a PWM splitter, if you need to control all 4
	 * separately, this class is not for you.
	 * 
	 * @param leftJaguar
	 *            Left Jaguar/SpeedController object
	 * @param rightJaguar
	 *            Right Jaguar/SpeedController object
	 * @param RC
	 *            RC Constant for Low Pass Filter
	 */
	private DriveTrain(SpeedController leftJaguar, SpeedController rightJaguar,
			Encoder leftEncoder, Encoder rightEncoder, double RC) {
		m_leftJaguar = leftJaguar;
		m_rightJaguar = rightJaguar;
		m_leftLP = new LowPassFilter(RC);
		m_rightLP = new LowPassFilter(RC);
		m_leftEnc = leftEncoder;
		m_rightEnc = rightEncoder;
	}

	/**
	 * Sets a RC value. Used when tuning with the analog controls.
	 * 
	 * @param RC
	 *            the RC value
	 */
	public void setRC(double RC) {
		m_leftLP.setRC(RC);
		m_rightLP.setRC(RC);
	}

	/**
	 * Gets the current RC value;
	 * 
	 * @return The current RC value set.
	 */
	public double getRC() {
		return m_leftLP.getRC();
	}

	/**
	 * Limit the max speed. Used for precision mode. You can also use this to
	 * reverse directions (negatives)
	 * 
	 * @param limit
	 *            A double between -1 - 1, as a percentage
	 */
	public void limitSpeed(double limit) {
		m_limit = limit;
	}

	/**
	 * Limit the left side, used for if one side is more powerful. You can use
	 * this to reverse directions (negatives)
	 * 
	 * @param limit
	 *            A double between -1 - 1, as a percentage
	 */
	public void limitLeft(double limit) {
		m_leftLimit = limit;
	}

	/**
	 * Limit the left side, used for if one side is more powerful. You can use
	 * this to reverse directions (negatives)
	 * 
	 * @param limit
	 *            A double between -1 - 1, as a percentage
	 */
	public void limitRight(double limit) {
		m_rightLimit = limit;
	}

	/**
	 * Stops robot by setting the speed of the controller to 0 (remember that
	 * the Jag should be on brake mode)
	 */
	public void hardBreak() {
		m_leftJaguar.set(0);
		m_rightJaguar.set(0);
		m_leftLP.reset(); // TODO: This was not present during the competition.
							// Is it required?
		m_rightLP.reset();
	}

	/**
	 * Gets degrees turned. Returns negative degrees for left turns, 
 	* and positive for right.
 	*/
	public double getTurnDegrees() {
		double circumference = Math.PI * 2 * Constants.DIST_BETWEEN_WHEELS;
		double leftDist = m_leftEnc.getDistance();
		double rightDist = m_rightEnc.getDistance();
		double angle = 360 * ((rightDist - leftDist)) / circumference;
		resetEncoders();
		return angle % 360;
	}

	private void resetEncoders() {
		m_rightEnc.reset();
		m_leftEnc.reset();
	}

	/**
	 * Gets distance traveled since last reset.
	 * This method will return an incorrect value if any turning has taken place
	 * since the last reset.
	 */
	public double getDistance() {
		double value = (m_leftEnc.getDistance() + m_rightEnc.getDistance()) / 2;
		resetEncoders();
		return value;
	}

	/**
	 * Tank drive. Controls the left and right speed. Not used usually.
	 * 
	 * @param leftSpeed
	 *            Left speed between -1 - 1
	 * @param rightSpeed
	 *            Right speed between -1 - 1
	 */
	public void tankDrive(double leftSpeed, double rightSpeed) {
		leftSpeed *= m_leftLimit * -m_limit;
		rightSpeed *= m_rightLimit * m_limit;

		leftSpeed = m_leftLP.calculate(leftSpeed);
		rightSpeed = m_rightLP.calculate(rightSpeed);

		m_leftJaguar.set(leftSpeed);
		m_rightJaguar.set(rightSpeed);
	}

	/**
	 * Arcade drive. It calculates the left and right speed.
	 * 
	 * @param moveValue
	 *            Value between -1 - 1
	 * @param rotateValue
	 *            Value between -1 - 1
	 */
	public void arcadeDrive(double moveValue, double rotateValue) {
		double leftMotorSpeed;
		double rightMotorSpeed;
		moveValue = (moveValue < 0 ? -(moveValue * moveValue)
				: (moveValue * moveValue));
		rotateValue = (rotateValue < 0 ? -(rotateValue * rotateValue)
				: (rotateValue * rotateValue));
		if (moveValue > 0.0) {
			if (rotateValue > 0.0) {
				leftMotorSpeed = moveValue - rotateValue;
				rightMotorSpeed = Math.max(moveValue, rotateValue);
			} else {
				leftMotorSpeed = Math.max(moveValue, -rotateValue);
				rightMotorSpeed = moveValue + rotateValue;
			}
		} else if (rotateValue > 0.0) {
			leftMotorSpeed = -Math.max(-moveValue, rotateValue);
			rightMotorSpeed = moveValue + rotateValue;
		} else {
			leftMotorSpeed = moveValue - rotateValue;
			rightMotorSpeed = -Math.max(-moveValue, -rotateValue);
		}
		tankDrive(leftMotorSpeed, rightMotorSpeed);
	}
}
