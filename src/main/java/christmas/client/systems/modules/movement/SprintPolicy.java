package christmas.client.systems.modules.movement;

/** Evaluates sprint eligibility from one consistent movement snapshot. */
public final class SprintPolicy {
    private SprintPolicy() {}

    public record State(boolean active, boolean rage, boolean screenAllowed, boolean forwardImpulse,
                        float forward, float sideways, boolean stationary, boolean stopInWater,
                        boolean inWater, boolean underwater, boolean shallowWater, boolean flying,
                        boolean mobilityRestricted, boolean passenger, boolean vehicleCanSprint,
                        boolean enoughFood, boolean collision, boolean minorCollision,
                        boolean slowedByItem, boolean fallFlying, boolean movingSlowly) {}

    public static boolean shouldSprint(State state) {
        if (!state.active() || !state.screenAllowed()) return false;
        if (state.rage()) {
            if (state.stopInWater() && state.inWater()) return false;
            float movement = Math.abs(state.forward()) + Math.abs(state.sideways());
            float threshold = state.underwater() ? 1.0E-5F : 0.8F;
            return state.stationary() || movement > threshold;
        }
        if (!state.forwardImpulse() || state.mobilityRestricted()) return false;
        if (state.shallowWater() && !state.flying()) return false;
        if (state.passenger() ? !state.vehicleCanSprint() : !state.enoughFood()) return false;
        if (state.collision() && !state.minorCollision()) return false;
        if (state.slowedByItem()) return false;
        if (state.fallFlying() && !state.underwater()) return false;
        return !state.movingSlowly() || state.underwater();
    }
}
