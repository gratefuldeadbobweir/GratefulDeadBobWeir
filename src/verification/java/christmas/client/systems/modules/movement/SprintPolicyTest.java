package christmas.client.systems.modules.movement;

public final class SprintPolicyTest {
    private SprintPolicyTest() {}

    private static final class Inputs {
        boolean active = true, rage, screenAllowed = true, forwardImpulse = true;
        float forward = 1, sideways;
        boolean stationary, stopInWater = true, inWater, underwater, shallowWater, flying;
        boolean mobilityRestricted, passenger, vehicleCanSprint, enoughFood = true;
        boolean collision, minorCollision, slowedByItem, fallFlying, movingSlowly;

        boolean sprint() {
            return SprintPolicy.shouldSprint(new SprintPolicy.State(active, rage, screenAllowed, forwardImpulse,
                forward, sideways, stationary, stopInWater, inWater, underwater, shallowWater, flying,
                mobilityRestricted, passenger, vehicleCanSprint, enoughFood, collision, minorCollision,
                slowedByItem, fallFlying, movingSlowly));
        }
    }

    private static void check(boolean actual, boolean expected, String message) {
        if (actual != expected) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }

    public static void main(String[] args) {
        Inputs i = new Inputs();
        check(i.sprint(), true, "forward");
        i.forward = 0.707F; i.sideways = 0.707F;
        check(i.sprint(), true, "forward diagonal");
        i.forwardImpulse = false;
        check(i.sprint(), false, "strict sideways only");
        i.forwardImpulse = true;
        i.mobilityRestricted = true; check(i.sprint(), false, "mobility restriction"); i.mobilityRestricted = false;
        i.shallowWater = true; check(i.sprint(), false, "shallow water");
        i.flying = true; check(i.sprint(), true, "creative flight in shallow water"); i.shallowWater = false; i.flying = false;
        i.enoughFood = false; check(i.sprint(), false, "hunger");
        i.passenger = true; i.vehicleCanSprint = true; check(i.sprint(), true, "sprintable vehicle independent of hunger");
        i.vehicleCanSprint = false; i.enoughFood = true; check(i.sprint(), false, "vehicle cannot sprint"); i.passenger = false;
        i.collision = true; check(i.sprint(), false, "wall collision");
        i.minorCollision = true; check(i.sprint(), true, "minor collision"); i.collision = false;
        i.slowedByItem = true; check(i.sprint(), false, "item prevents sprint"); i.slowedByItem = false;
        i.fallFlying = true; check(i.sprint(), false, "gliding");
        i.underwater = true; check(i.sprint(), true, "gliding underwater"); i.fallFlying = false;
        i.movingSlowly = true; check(i.sprint(), true, "underwater movement");
        i.underwater = false; check(i.sprint(), false, "sneak or crawl"); i.movingSlowly = false;
        i.screenAllowed = false; check(i.sprint(), false, "disallowed screen");
        i.screenAllowed = true; i.active = false; check(i.sprint(), false, "inactive");
        i.active = true; i.rage = true; i.forwardImpulse = false; i.forward = 0; i.sideways = 1;
        i.enoughFood = false; i.mobilityRestricted = true; i.collision = true;
        check(i.sprint(), true, "rage sideways ignores strict conditions");
        i.inWater = true; check(i.sprint(), false, "rage stop in water");
        i.stopInWater = false; check(i.sprint(), true, "rage water enabled");
        i.forward = 0; i.sideways = 0; check(i.sprint(), false, "rage stationary disabled");
        i.stationary = true; check(i.sprint(), true, "rage stationary enabled");
        i.stopInWater = true; check(i.sprint(), false, "water overrides stationary");
        i.inWater = false; i.screenAllowed = false; check(i.sprint(), false, "screen overrides rage");
        System.out.println("SprintPolicy regression suite passed");
    }
}
