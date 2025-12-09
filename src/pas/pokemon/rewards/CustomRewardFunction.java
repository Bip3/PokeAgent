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
    private static final double DAMAGE_TAKEN_WEIGHT = -0.5;
    private static final double FAINT_OPPONENT_REWARD = 2.0;
    private static final double FAINT_SELF_PENALTY = -2.0;
    private static final double HP_DIFFERENTIAL_WEIGHT = 0.3;
    private static final double STATUS_INFLICT_REWARD = 0.3;
    private static final double STATUS_RECEIVE_PENALTY = -0.3;
    private static final double TYPE_ADVANTAGE_BONUS = 0.1;
    private static final double STEP_PENALTY = -0.01;

    public CustomRewardFunction() {
        super(RewardType.STATE_ACTION_STATE);
    }

    @Override
    public double getLowerBound() {
        return LOSS_REWARD + FAINT_SELF_PENALTY * 6 + DAMAGE_TAKEN_WEIGHT * 5;
    }

    @Override
    public double getUpperBound() {
        return WIN_REWARD + FAINT_OPPONENT_REWARD * 6 + DAMAGE_DEALT_WEIGHT * 5;
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
        // Determine which team is mine
        int myTeam = this.getMyTeamIdx();

        TeamView myTeamEnd  = (myTeam == 0) ? nextState.getTeam1View()
                                            : nextState.getTeam2View();
        TeamView oppTeamEnd = (myTeam == 0) ? nextState.getTeam2View()
                                            : nextState.getTeam1View();

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

        if (!oppStatusBefore.equals(oppStatusAfter) && !oppStatusAfter.equals("NONE")) {
            reward += STATUS_INFLICT_REWARD;
        }

        // Status Effect Penalty
        String myStatusBefore = myPokemonBefore.getNonVolatileStatus().toString();
        String myStatusAfter = myPokemonAfter.getNonVolatileStatus().toString();

        if (!myStatusBefore.equals(myStatusAfter) && !myStatusAfter.equals("NONE")) {
            reward += STATUS_RECEIVE_PENALTY;
        }

        // Super Effective Moves!
        Integer actionPower = action.getPower();
        if (actionPower != null && actionPower > 0) {
            double effectiveness = Type.getEffectivenessModifier(action.getType(), oppPokemonBefore.getCurrentType1());
            if (oppPokemonBefore.getCurrentType2() != null) {
                effectiveness *= Type.getEffectivenessModifier(action.getType(), oppPokemonBefore.getCurrentType2());
            }

            if (effectiveness > 1.0) {
                reward += TYPE_ADVANTAGE_BONUS * effectiveness;
            }
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
