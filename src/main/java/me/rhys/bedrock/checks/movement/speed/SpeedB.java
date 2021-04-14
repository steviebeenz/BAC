package me.rhys.bedrock.checks.movement.speed;

import me.rhys.bedrock.base.check.api.Check;
import me.rhys.bedrock.base.check.api.CheckInformation;
import me.rhys.bedrock.base.event.PacketEvent;
import me.rhys.bedrock.base.user.User;
import me.rhys.bedrock.tinyprotocol.api.Packet;
import me.rhys.bedrock.util.EventTimer;

@CheckInformation(checkName = "Speed", checkType = "B")
public class SpeedB extends Check {

    private EventTimer lastJumpTimer;
    private double groundThreshold, airThreshold;

    @Override
    public void onPacket(PacketEvent event) {
        switch (event.getType()) {
            case Packet.Client.FLYING:
            case Packet.Client.LOOK:
            case Packet.Client.POSITION_LOOK:
            case Packet.Client.POSITION: {
                User user = event.getUser();

                if (!this.checkConditions(user)) {
                    double deltaXZ = user.getMovementProcessor().getDeltaXZ();
                    this.processDeltaY(user);

                    Tags tag = this.findTag(user);

                    switch (tag) {
                        case GROUND: {
                            boolean expand = this.lastJumpTimer.hasNotPassed();

                            //Not the best but will do the job for now.
                            double max = (expand ? .6325 : .29);

                            if (user.getPotionProcessor().getSpeedTicks() > 0) {
                                max += (user.getPotionProcessor().getSpeedAmplifier() * .2);
                            }

                            if (deltaXZ > max) {
                                if ((this.groundThreshold += 1.2) > 3.5) {
                                    this.flag(user,
                                            "tag: " + tag.name(),
                                            "speed: " + deltaXZ,
                                            "max: " + max,
                                            "threshold: " + this.groundThreshold,
                                            "expand: " + expand
                                    );
                                }
                            } else {
                                this.groundThreshold -= (this.groundThreshold > 0 ? 0.4 : 0);
                            }
                            break;
                        }

                        case AIR: {
                            if (user.getMovementProcessor().getServerGroundTicks() == 0) {
                                double max = (user.getPotionProcessor().getSpeedTicks() > 0
                                        ? .3655 + (user.getPotionProcessor().getSpeedAmplifier() * .030) : .3655);

                                if (user.getBlockData().iceTicks > 0) {
                                    max += .1922;
                                }

                                if (deltaXZ > max && (this.airThreshold += 1.1) > 2.94) {
                                    this.flag(user,
                                            "tag: " + tag.name(),
                                            "speed: " + deltaXZ,
                                            "max: " + max,
                                            "threshold: " + this.airThreshold,
                                            "at: " + user.getMovementProcessor().getAirTicks()
                                    );
                                }
                            } else {
                                this.airThreshold -= (this.airThreshold > 0 ? .6 : 0);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setupTimers(User user) {
        this.lastJumpTimer = new EventTimer(20, user);
    }

    boolean checkConditions(User user) {
        return user.shouldCancel() || ((user.getBlockData().movingUpTimer.hasNotPassed()
                || Math.abs(user.getMovementProcessor().getDeltaY()) > 0)
                && (user.getBlockData().slabTicks > 0 || user.getBlockData().stairTicks > 0))
                || user.getActionProcessor().getServerPositionTimer().hasNotPassed()
                || user.getCombatProcessor().getPreVelocityTimer().hasNotPassed();
    }

    void processDeltaY(User user) {
        double deltaY = user.getMovementProcessor().getDeltaY();

        //Doesn't make any sense, but ok bro
        if (!user.getMovementProcessor().isOnGround()
                && user.getMovementProcessor().isLastGround() && deltaY < 0.009) {
            this.lastJumpTimer.reset();
        }
    }

    Tags findTag(User user) {
        return (user.getBlockData().onGround ? Tags.GROUND : Tags.AIR);
    }

    public enum Tags {
        GROUND,
        AIR
    }
}
