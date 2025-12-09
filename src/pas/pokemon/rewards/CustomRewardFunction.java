package src.pas.pokemon.rewards;

// SYSTEM IMPORTS

// JAVA PROJECT IMPORTS
import edu.bu.pas.pokemon.agents.rewards.RewardFunction;
import edu.bu.pas.pokemon.agents.rewards.RewardFunction.RewardType;
import edu.bu.pas.pokemon.core.Battle.BattleView;
import edu.bu.pas.pokemon.core.Move.MoveView;
import edu.bu.pas.pokemon.core.Pokemon.PokemonView;
import edu.bu.pas.pokemon.core.Team.TeamView;
import edu.bu.pas.pokemon.core.enums.Stat;

public class CustomRewardFunction extends RewardFunction {

    // Terminal rewards - INCREASED to match reference
    private static final double WIN_REWARD  = 50.0;
    private static final double LOSS_REWARD = -50.0;

    // Core reward weights - SIMPLIFIED to basics only
    private static final double DAMAGE_DEALT_WEIGHT = 10.0;
    private static final double DAMAGE_TAKEN_WEIGHT = -10.0;
    private static final double FAINT_OPPONENT_REWARD = 10.0;
    private static final double FAINT_SELF_PENALTY = -10.0;

    public CustomRewardFunction() {
        super(RewardType.STATE_ACTION_STATE);
    }

    @Override
    public double getLowerBound() {
        return LOSS_REWARD + FAINT_SELF_PENALTY * 6 + DAMAGE_TAKEN_WEIGHT * 6;
    }

    @Override
    public double getUpperBound() {
        return WIN_REWARD + FAINT_OPPONENT_REWARD * 6 + DAMAGE_DEALT_WEIGHT * 6;
    }

    @Override
    public double getStateReward(final BattleView state) {
        return 0;
    }

    @Override
    public double getStateActionReward(final BattleView state,
                                       final MoveView action) {
        return 0;
    }

    @Override
    public double getStateActionStateReward(final BattleView state,
                                            final MoveView action,
                                            final BattleView nextState) {
        double reward = 0.0;

        // 1. TERMINAL STATES: Win/Loss
        if (nextState.isOver()) {
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

        // Get teams and active pokemon
        TeamView myTeamBefore = state.getTeam1View();
        TeamView oppTeamBefore = state.getTeam2View();
        TeamView myTeamAfter = nextState.getTeam1View();
        TeamView oppTeamAfter = nextState.getTeam2View();

        PokemonView myPokemonBefore = myTeamBefore.getActivePokemonView();
        PokemonView oppPokemonBefore = oppTeamBefore.getActivePokemonView();
        PokemonView myPokemonAfter = myTeamAfter.getActivePokemonView();
        PokemonView oppPokemonAfter = oppTeamAfter.getActivePokemonView();

        // 2. DAMAGE DEALT to opponent
        int oppHPBefore = oppPokemonBefore.getCurrentStat(Stat.HP);
        int oppHPAfter = oppPokemonAfter.getCurrentStat(Stat.HP);
        int oppMaxHP = oppPokemonBefore.getInitialStat(Stat.HP);

        if (oppHPAfter < oppHPBefore) {
            int damageTaken = oppHPBefore - oppHPAfter;
            reward += DAMAGE_DEALT_WEIGHT * ((double) damageTaken / oppMaxHP);
        }

        // 3. DAMAGE TAKEN (penalty)
        int myHPBefore = myPokemonBefore.getCurrentStat(Stat.HP);
        int myHPAfter = myPokemonAfter.getCurrentStat(Stat.HP);
        int myMaxHP = myPokemonBefore.getInitialStat(Stat.HP);

        if (myHPAfter < myHPBefore) {
            int damageTaken = myHPBefore - myHPAfter;
            reward += DAMAGE_TAKEN_WEIGHT * ((double) damageTaken / myMaxHP);
        }

        // 4. FAINTED POKEMON checks
        int myFaintedBefore = countFaintedPokemon(myTeamBefore);
        int myFaintedAfter = countFaintedPokemon(myTeamAfter);
        int oppFaintedBefore = countFaintedPokemon(oppTeamBefore);
        int oppFaintedAfter = countFaintedPokemon(oppTeamAfter);

        // Reward for fainting opponent pokemon
        if (oppFaintedAfter > oppFaintedBefore) {
            reward += FAINT_OPPONENT_REWARD * (oppFaintedAfter - oppFaintedBefore);
        }

        // Penalty for losing own pokemon
        if (myFaintedAfter > myFaintedBefore) {
            reward += FAINT_SELF_PENALTY * (myFaintedAfter - myFaintedBefore);
        }

        return reward;
    }

    // Count the number of fainted Pokemon in a team
    private int countFaintedPokemon(TeamView team) {
        int fainted = 0;
        for (int i = 0; i < team.size(); i++) {
            if (team.getPokemonView(i).hasFainted()) {
                fainted++;
            }
        }
        return fainted;
    }
}
