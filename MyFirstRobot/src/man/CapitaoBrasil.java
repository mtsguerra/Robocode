package man;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.*;

public class CapitaoBrasil extends AdvancedRobot {
    private Random random = new Random();
    private double enemyDistance;
    // the buffer to form an inside polygon with 15% of the original size
    public final double PERCENT_BUFFER = 0.15;
    private boolean isMovingForward = true;


    public void run() {
        // robot set up
        // BRASIL !!!!!!!!
        setColors(Color.GREEN, Color.YELLOW, Color.BLUE, Color.GREEN, Color.BLUE);
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);
        setTurnRadarRight(Double.POSITIVE_INFINITY);

        while (true) {
            if (getRadarTurnRemaining() == 0) {
                setTurnRadarRight(360);
            }

            // when no enemies detected just do random movements
            if (random.nextInt(20) == 0) {
                isMovingForward = !isMovingForward;
            }
            if (random.nextInt(30) == 0) {
                setTurnRight(random.nextInt(110) - 45);
            }
            // sometimes changing directions
            setAhead(isMovingForward ? 100 : -100);

            // check for walls
            avoidWalls();
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        // lock the radar on the enemy
        // by using the absolute bearing of the enemy and the current heading
        // by subtracting and finding it relative angle to use it on the turn
        double radarTurn = Utils.normalRelativeAngleDegrees(getHeading() + e.getBearing() - getRadarHeading());
        // guarantee the radar keeps on tracking the enemy
        setTurnRadarRight(radarTurn * 1.5);

        enemyDistance = e.getDistance();

        moveEvasively(e);
        aimAndShoot(e);
    }

    private void moveEvasively(ScannedRobotEvent e) {
        // try to keep this distance from the enemy
        double optimalDistance = 200;
        double distanceError = e.getDistance() - optimalDistance;

        // moves towards the enemy or away from the enemy depending on its distance
        // only changes if the distanceError is relevant
        if (Math.abs(distanceError) > 20) {
            // case 1: it's too far away from the enemy
            if (distanceError > 0) {
                setTurnRight(e.getBearing());
                // moves 50% of the error into the enemy
                setAhead(distanceError * 0.5);
            }
            // case 2: too close, it needs to get away
            else {
                // -180 to "flip it"
                setTurnRight(e.getBearing() - 180);
                // gets back 50%
                setAhead(-distanceError * 0.5);
            }
        }
        // the distance is ideal, so it keeps perpendicular to it
        else {
            // +90 to keeps perpendicular
            setTurnRight(Utils.normalRelativeAngleDegrees(e.getBearing() + 90));
            // 100 pxs relative to the boolean movingForward
            setAhead(100 * (isMovingForward ? 1 : -1));

            // changing directions at random
            if (random.nextInt(22) == 0) {
                isMovingForward = !isMovingForward;
            }
        }
    }

    private void aimAndShoot(ScannedRobotEvent e) {
        // adjust the power of the bullet with the distance
        double bulletPower = Math.min(400 / enemyDistance, 3);
        if (enemyDistance < 50) bulletPower = 3; // max power for close targets
        else if (enemyDistance > 400) bulletPower = 1; // min power for distant targets

        double absoluteBearing = getAbsoluteBearing(e, bulletPower);

        // turn gun to the predicted position
        double gunTurn = Utils.normalRelativeAngleDegrees(absoluteBearing - getGunHeading());
        setTurnGunRight(gunTurn);

        // fire when gun is pointed at target and its heat is 0
        if (Math.abs(getGunTurnRemaining()) < 10 && getGunHeat() == 0) {
            setFire(bulletPower);
        }
    }

    private double getAbsoluteBearing(ScannedRobotEvent e, double bulletPower) {
        double absoluteBearing = getHeading() + e.getBearing();

        // if enemy is moving, lead the target based on its velocity
        if (Math.abs(e.getVelocity()) > 0) {
            double enemyHeading = e.getHeading();
            double enemyVelocity = e.getVelocity();

            // prediction of the movement
            // 20-(3*bulletPower) calculates the speed of the bullet
            // and the eVelocity / bulletSpeed calculates the ratio between them
            // it converts to radians as the math.sin only works with radians
            // after that uses the math.asin to calculate the angle
            // and in the ends uses the *180 / pi to convert back to degrees
            double leadAngle = Math.asin(enemyVelocity / (20 - (3 * bulletPower)) *
                    Math.sin(Math.toRadians(enemyHeading - absoluteBearing)))
                    * 180 / Math.PI;
            // it adds the leadAngle to the absolute to predict the direction
            absoluteBearing += leadAngle;
        }
        return absoluteBearing;
    }

    public void onHitByBullet(HitByBulletEvent e) {
        // dodges perpendicular to the bullet direction
        double bulletBearing = e.getBearing();
        setTurnRight(Utils.normalRelativeAngleDegrees(90 - bulletBearing));
        // also changing direction
        isMovingForward = !isMovingForward;
        setAhead(isMovingForward ? 150 : -150);
    }

    public void onHitWall(HitWallEvent e) {
        // just turn directions
        setTurnRight(Utils.normalRelativeAngleDegrees(180 - e.getBearing()));
        isMovingForward = !isMovingForward;
        setAhead(isMovingForward ? 150 : -150);
    }

    private void avoidWalls() {
        double width = getBattleFieldWidth();
        double height = getBattleFieldHeight();
        double buffer = PERCENT_BUFFER * Math.max(width, height);
        double x = getX();
        double y = getY();

        // finds which wall we are closing in and the best escape route, if it's found
        // bottom wall, it goes from 90 to 270
        if (y < buffer) {
            if (getHeading() >= 180 && getHeading() <= 270){
                setTurnRight(90);
            }
            else if (getHeading() >= 90 && getHeading() < 180){
                setTurnLeft(90);
            }
        }
        // top wall, it goes from 0 to 90 and 270 to 360
        else if (y > height - buffer) {
            if (getHeading() >= 0 && getHeading() <= 90){
                setTurnRight(90);
            }
            else if (getHeading() >= 270 && getHeading() < 360){
                setTurnLeft(90);
            }
        }
        // left wall, it goes from 180 to 360 degrees
        else if (x < buffer) {
            if (getHeading() >= 270 && getHeading() <= 360){
                setTurnRight(90);
            }
            else if (getHeading() >= 180 && getHeading() < 270){
                setTurnLeft(90);
            }
        }
        // right wall, it goes from 0 to 180 degrees
        else if (x > width - buffer) {
            if (getHeading() >= 90 && getHeading() <= 180){
                setTurnRight(90);
            }
            else if (getHeading() >= 0 && getHeading() < 90){
                setTurnLeft(90);
            }
        }

        setAhead(100);
        isMovingForward = true;
    }

    public void onHitRobot(HitRobotEvent e) {
        // back up and turn when hitting another robot
        if (e.isMyFault()) {
            setTurnRight(Utils.normalRelativeAngleDegrees(e.getBearing() + 180)); //turn half round
            setBack(100); //move back
        }
        else {
            // if the other robot hits turn around
            double absBearing = e.getBearing() + getHeading();
            setTurnGunRight(Utils.normalRelativeAngleDegrees(absBearing - getGunHeading())); //redirect gun at enemy
            if (getGunHeat() == 0) {
                setFire(3); //shoot enemy if not shooting
            }
            setTurnRight(Utils.normalRelativeAngleDegrees(e.getBearing() + 90)); //move another direction
            setAhead(100);// leave the encounter
        }
    }

    public void onWin(WinEvent e) {
        // victory dance
        // SPINS !!!
        for (int i = 0; i < 50; i++) {
            setTurnRight(360);
            setTurnGunRight(360);
            setTurnRadarRight(360);
            execute();
        }
    }
}