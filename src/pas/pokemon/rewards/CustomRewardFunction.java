package src.pas.pokemon.rewards;

// SYSTEM IMPORTS

// JAVA PROJECT IMPORTS
import edu.bu.pas.pokemon.agents.rewards.RewardFunction;
import edu.bu.pas.pokemon.agents.rewards.RewardFunction.RewardType;
import edu.bu.pas.pokemon.core.Battle.BattleView;
import edu.bu.pas.pokemon.core.Move.MoveView;
import edu.bu.pas.pokemon.core.Pokemon.PokemonView;
import edu.bu.pas.pokemon.core.Team.TeamView;
import edu.bu.pas.pokemon.core.enums.Type;
import edu.bu.pas.pokemon.core.enums.Stat;

public class CustomRewardFunction extends RewardFunction {

    // Terminal rewards
    private static final double WIN_REWARD  = 10.0;
    private static final double LOSS_REWARD = -10.0;

    // IM reward weights
    private static final double DAMAGE_DEALT_WEIGHT = 0.5;
    private static final double DAMAGE_TAKEN_WEIGHT = -0.3;
    private static final double FAINT_OPPONENT_REWARD = 2.0;
    private static final double FAINT_SELF_PENALTY = -2.0;
    private static final double HP_DIFFERENTIAL_WEIGHT = 0.3;
    private static final double STATUS_INFLICT_REWARD = 0.3;
    private static final double STATUS_RECEIVE_PENALTY = -0.1;
    private static final double TYPE_ADVANTAGE_BONUS = 1;
    private static final double STEP_PENALTY = -0.01;

    // New strategic reward weights
    private static final double OFFENSIVE_STATUS_BONUS = 0.5;  // Extra reward for offensive status effects
    private static final double COMBO_BONUS = 1.5;  // Bonus for super effective + offensive status combo
    private static final double REDUNDANT_STATUS_PENALTY = -1.0;  // Penalty for redundant status moves
    private static final double HIGH_POWER_MOVE_BONUS = 0.8;  // Bonus for high power moves (>= 80 power)
    private static final double EFFECTIVE_SWITCH_BONUS = 0.7;  // Bonus for switching to effective attacker after status

    public CustomRewardFunction() {
        super(RewardType.STATE_ACTION_STATE);
    }

    @Override
    public double getLowerBound() {
        return LOSS_REWARD + FAINT_SELF_PENALTY * 6 + DAMAGE_TAKEN_WEIGHT * 5 +
               REDUNDANT_STATUS_PENALTY * 3 + STATUS_RECEIVE_PENALTY * 6;
    }

    @Override
    public double getUpperBound() {
        return WIN_REWARD + FAINT_OPPONENT_REWARD * 6 + DAMAGE_DEALT_WEIGHT * 5 +
               TYPE_ADVANTAGE_BONUS * 2 + COMBO_BONUS * 6 + OFFENSIVE_STATUS_BONUS * 6 +
               HIGH_POWER_MOVE_BONUS * 6 + EFFECTIVE_SWITCH_BONUS * 3;
    }

    @Override
    public double getStateReward(final BattleView state) {
        //not used
        return STEP_PENALTY;
    }

    @Override
    public double getStateActionReward(final BattleView state,
                                       final MoveView action) {
        // not used
        return STEP_PENALTY;
    }

    @Override
    public double getStateActionStateReward(final BattleView state,
                                            final MoveView action,
                                            final BattleView nextState) {
        double reward = 0.0;

        // Check for terminal states
    if (nextState.isOver()) {
        // Determine which team is mine (Team 1 is us, Team 2 is opponent)
        TeamView myTeamEnd = nextState.getTeam1View();
        TeamView oppTeamEnd = nextState.getTeam2View();

        boolean iWon = countFaintedPokemon(oppTeamEnd) == oppTeamEnd.size()
                    && countFaintedPokemon(myTeamEnd) < myTeamEnd.size();

        if (iWon) {
            return WIN_REWARD;
        } else {
            return LOSS_REWARD;
        }
    }


        // Get my team and opponents team
        TeamView myTeamBefore = state.getTeam1View();
        TeamView oppTeamBefore = state.getTeam2View();
        TeamView myTeamAfter = nextState.getTeam1View();
        TeamView oppTeamAfter = nextState.getTeam2View();

        PokemonView myPokemonBefore = myTeamBefore.getActivePokemonView();
        PokemonView oppPokemonBefore = oppTeamBefore.getActivePokemonView();
        PokemonView myPokemonAfter = myTeamAfter.getActivePokemonView();
        PokemonView oppPokemonAfter = oppTeamAfter.getActivePokemonView();

        // Dmg reward on opponent
        int oppHPBefore = oppPokemonBefore.getCurrentStat(Stat.HP);
        int oppHPAfter = oppPokemonAfter.getCurrentStat(Stat.HP);
        int oppMaxHP = oppPokemonBefore.getInitialStat(Stat.HP);

        if (oppHPAfter < oppHPBefore) {
            int damageTaken = oppHPBefore - oppHPAfter;
            reward += DAMAGE_DEALT_WEIGHT * ((double) damageTaken / oppMaxHP);
        }

        // Dmg penalty
        int myHPBefore = myPokemonBefore.getCurrentStat(Stat.HP);
        int myHPAfter = myPokemonAfter.getCurrentStat(Stat.HP);
        int myMaxHP = myPokemonBefore.getInitialStat(Stat.HP);

        if (myHPAfter < myHPBefore) {
            int damageTaken = myHPBefore - myHPAfter;
            reward += DAMAGE_TAKEN_WEIGHT * ((double) damageTaken / myMaxHP);
        }


        // Fainted Pokemon check
        int myFaintedBefore = countFaintedPokemon(myTeamBefore);
        int myFaintedAfter = countFaintedPokemon(myTeamAfter);
        int oppFaintedBefore = countFaintedPokemon(oppTeamBefore);
        int oppFaintedAfter = countFaintedPokemon(oppTeamAfter);

        // Give a reward for killing an enemy pokemon
        if (oppFaintedAfter > oppFaintedBefore) {
            reward += FAINT_OPPONENT_REWARD * (oppFaintedAfter - oppFaintedBefore);
        }

        // Penalty for losing a pokemon
        if (myFaintedAfter > myFaintedBefore) {
            reward += FAINT_SELF_PENALTY * (myFaintedAfter - myFaintedBefore);
        }

        // Reward for having more HP
        double myTeamHPBefore = calculateTeamHPPercentage(myTeamBefore);
        double oppTeamHPBefore = calculateTeamHPPercentage(oppTeamBefore);
        double myTeamHPAfter = calculateTeamHPPercentage(myTeamAfter);
        double oppTeamHPAfter = calculateTeamHPPercentage(oppTeamAfter);

        double hpDiffBefore = myTeamHPBefore - oppTeamHPBefore;
        double hpDiffAfter = myTeamHPAfter - oppTeamHPAfter;
        double hpDiffChange = hpDiffAfter - hpDiffBefore;

        reward += HP_DIFFERENTIAL_WEIGHT * hpDiffChange;

        // Status Effect Rewards
        String oppStatusBefore = oppPokemonBefore.getNonVolatileStatus().toString();
        String oppStatusAfter = oppPokemonAfter.getNonVolatileStatus().toString();
        String myStatusBefore = myPokemonBefore.getNonVolatileStatus().toString();
        String myStatusAfter = myPokemonAfter.getNonVolatileStatus().toString();

        // Check for redundant status application (trying to inflict status on already statused pokemon)
        if (!oppStatusBefore.equals("NONE") && !oppStatusBefore.equals(oppStatusAfter)) {
            // Trying to change an existing status - penalize this
            reward += REDUNDANT_STATUS_PENALTY;
        } else if (!oppStatusBefore.equals(oppStatusAfter) && !oppStatusAfter.equals("NONE")) {
            // Successfully inflicted a new status
            reward += STATUS_INFLICT_REWARD;

            // Extra bonus for offensive status effects
            if (isOffensiveStatus(oppStatusAfter)) {
                reward += OFFENSIVE_STATUS_BONUS;
            }
        }

        // Penalty for receiving status
        if (!myStatusBefore.equals(myStatusAfter) && !myStatusAfter.equals("NONE")) {
            reward += STATUS_RECEIVE_PENALTY;
        }

        // Super Effective Moves and High Power Move Rewards
        Integer actionPower = action.getPower();
        boolean isSuperEffective = false;

        if (actionPower != null && actionPower > 0) {
            double effectiveness = Type.getEffectivenessModifier(action.getType(), oppPokemonBefore.getCurrentType1());
            if (oppPokemonBefore.getCurrentType2() != null) {
                effectiveness *= Type.getEffectivenessModifier(action.getType(), oppPokemonBefore.getCurrentType2());
            }

            if (effectiveness > 1.0) {
                reward += TYPE_ADVANTAGE_BONUS * effectiveness;
                isSuperEffective = true;
            }

            // Reward high power moves (>= 80 power)
            if (actionPower >= 80) {
                reward += HIGH_POWER_MOVE_BONUS;
            }

            // Combo bonus: Super effective attack on pokemon with offensive status
            if (isSuperEffective && isOffensiveStatus(oppStatusBefore)) {
                reward += COMBO_BONUS;
            }
        }

        // Reward switching to a more effective attacker after opponent has offensive status
        if (action != null && action.getName().startsWith("SWITCH_")) {
            if (isOffensiveStatus(oppStatusBefore)) {
                // Check if the new pokemon has type advantage
                PokemonView newPokemon = myPokemonAfter;
                if (!newPokemon.equals(myPokemonBefore)) {
                    // Switched to a different pokemon, check for potential type advantage
                    // We'll give a bonus if the new pokemon could be more effective
                    // This encourages switching to better attackers after status is applied
                    reward += EFFECTIVE_SWITCH_BONUS;
                }
            }
        }
	//Stopping Infinite Stalling
	if (action != null && action.getName().startsWith("SWITCH_")) {
		reward += -.01;
	}

        // Reduces stalling by adding a penalty. This bitch is a glass cannon
        reward += STEP_PENALTY;

        return reward;
    }

     //Count the number of fainted Pokemon in a team
    private int countFaintedPokemon(TeamView team) {
        int fainted = 0;
        for (int i = 0; i < team.size(); i++) {
            if (team.getPokemonView(i).hasFainted()) {
                fainted++;
            }
        }
        return fainted;
    }

    // Check if a status effect is offensive (damage-dealing or confusion)
    private boolean isOffensiveStatus(String status) {
        return status.equals("POISON") || status.equals("TOXIC") ||
               status.equals("BURN") || status.equals("CONFUSION");
    }

     //Getting HP for all pokemon in a team.
    private double calculateTeamHPPercentage(TeamView team) {
        double totalHP = 0.0;
        int totalPokemon = team.size();

        for (int i = 0; i < totalPokemon; i++) {
            PokemonView p = team.getPokemonView(i);
            int currentHP = p.getCurrentStat(Stat.HP);
            int maxHP = p.getInitialStat(Stat.HP);
            totalHP += (double) currentHP / maxHP;
        }

        return totalHP / totalPokemon;
    }
}
