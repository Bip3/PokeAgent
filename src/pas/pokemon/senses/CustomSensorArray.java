package src.pas.pokemon.senses;


// SYSTEM IMPORTS


// JAVA PROJECT IMPORTS
import edu.bu.pas.pokemon.agents.senses.SensorArray;
import edu.bu.pas.pokemon.core.Battle.BattleView;
import edu.bu.pas.pokemon.core.Move.MoveView;
import edu.bu.pas.pokemon.core.Pokemon.PokemonView;
import edu.bu.pas.pokemon.core.Team.TeamView;
import edu.bu.pas.pokemon.core.enums.Type;
import edu.bu.pas.pokemon.core.enums.Stat;
import edu.bu.pas.pokemon.core.Move.Category;
import edu.bu.pas.pokemon.linalg.Matrix;


public class CustomSensorArray
    extends SensorArray
{
    // My Pokemon: 20 features
    // Opponent Pokemon: 20 features
    // My Team: 6 features
    // Opponent Team: 6 features
    // Action/Move: 15 features
    // Type effectiveness: 3 features

    public static final int NUM_FEATURES = 70;

    public CustomSensorArray() {
        super();
    }

    public Matrix getSensorValues(final BattleView state, final MoveView action)
    {
        double[] features = new double[NUM_FEATURES];
        int idx = 0;

        // Get teams and active Pokemon
        TeamView myTeam = state.getTeam1View();
        TeamView oppTeam = state.getTeam2View();
        PokemonView myPokemon = myTeam.getActivePokemonView();
        PokemonView oppPokemon = oppTeam.getActivePokemonView();

        // Features for my team
        idx = extractPokemonFeatures(myPokemon, features, idx);

        // Features for enemy team
        idx = extractPokemonFeatures(oppPokemon, features, idx);

        // Status for my team
        idx = extractTeamFeatures(myTeam, features, idx);

        // Status for enemy team
        idx = extractTeamFeatures(oppTeam, features, idx);

        // Action features
        idx = extractMoveFeatures(action, myPokemon, features, idx);

        // Type features
        idx = extractTypeEffectiveness(action, myPokemon, oppPokemon, features, idx);

        Matrix sensorMatrix = Matrix.zeros(1, NUM_FEATURES);
        for (int i = 0; i < NUM_FEATURES; i++) {
            sensorMatrix.set(0, i, features[i]);
        }

        return sensorMatrix;
    }

 
    private int extractPokemonFeatures(PokemonView pokemon, double[] features, int startIdx) {
        int idx = startIdx;

        // HP features
        int currentHP = pokemon.getCurrentStat(Stat.HP);
        int maxHP = pokemon.getInitialStat(Stat.HP);
        features[idx++] = (double) currentHP / maxHP;
        features[idx++] = (double) currentHP / 200.0;

        features[idx++] = (double) pokemon.getLevel() / 100.0; 

        features[idx++] = (pokemon.getStatMultiplier(Stat.ATK) + 6) / 12.0;
        features[idx++] = (pokemon.getStatMultiplier(Stat.DEF) + 6) / 12.0;
        features[idx++] = (pokemon.getStatMultiplier(Stat.SPD) + 6) / 12.0;
        features[idx++] = (pokemon.getStatMultiplier(Stat.SPATK) + 6) / 12.0;
        features[idx++] = (pokemon.getStatMultiplier(Stat.SPDEF) + 6) / 12.0;
        features[idx++] = (pokemon.getStatMultiplier(Stat.ACC) + 6) / 12.0;
        features[idx++] = (pokemon.getStatMultiplier(Stat.EVASIVE) + 6) / 12.0;

        String nvStatus = pokemon.getNonVolatileStatus().toString();
        features[idx++] = nvStatus.equals("PARALYSIS") ? 1.0 : 0.0;
        features[idx++] = nvStatus.equals("POISON") ? 1.0 : 0.0;
        features[idx++] = nvStatus.equals("TOXIC") ? 1.0 : 0.0;
        features[idx++] = nvStatus.equals("BURN") ? 1.0 : 0.0;
        features[idx++] = nvStatus.equals("FREEZE") ? 1.0 : 0.0;
        features[idx++] = nvStatus.equals("SLEEP") ? 1.0 : 0.0;

        //weird statuses that I dont think will really matter.
        features[idx++] = 0.0; // Focused
        features[idx++] = 0.0; // Confused
        features[idx++] = 0.0; // Trapped
        features[idx++] = 0.0; // Seeded

        return idx;
    }


    private int extractTeamFeatures(TeamView team, double[] features, int startIdx) {
        int idx = startIdx;

        int totalPokemon = team.size();
        int alivePokemon = 0;
        double totalHPPercent = 0.0;

        for (int i = 0; i < totalPokemon; i++) {
            PokemonView p = team.getPokemonView(i);
            if (!p.hasFainted()) {
                alivePokemon++;
                int currentHP = p.getCurrentStat(Stat.HP);
                int maxHP = p.getInitialStat(Stat.HP);
                totalHPPercent += (double) currentHP / maxHP;
            }
        }

        // Team features
        features[idx++] = (double) alivePokemon / totalPokemon; 
        features[idx++] = totalHPPercent / totalPokemon;
        features[idx++] = (double) alivePokemon / 6.0;
        features[idx++] = (double) totalPokemon / 6.0;

        features[idx++] = (double) team.getActivePokemonIdx() / totalPokemon;

        features[idx++] = totalHPPercent;

        return idx;
    }


    private int extractMoveFeatures(MoveView move, PokemonView myPokemon, double[] features, int startIdx) {
        int idx = startIdx;

        // Edge
        if (move == null || myPokemon == null) {
            for (int i = 0; i < 15; i++) {
                features[idx++] = 0.0;
            }
            return idx;
        }

        // Switch check
        boolean isSwitch = move.getName().startsWith("SWITCH_");
        features[idx++] = isSwitch ? 1.0 : 0.0;

        if (isSwitch) {
            // If we switch, that bitch gone
            for (int i = 0; i < 14; i++) {
                features[idx++] = 0.0;
            }
            return idx;
        }

        // Move damage
        Integer power = move.getPower();
        features[idx++] = (power != null) ? power / 250.0 : 0.0; // Max power ~250

        // Accuracy of the move
        Integer accuracy = move.getAccuracy();
        features[idx++] = (accuracy != null) ? accuracy / 100.0 : 1.0;

        // Move Prio
        features[idx++] = (move.getPriority() + 6.0) / 12.0;

        Category cat = move.getCategory();
        features[idx++] = (cat == Category.PHYSICAL) ? 1.0 : 0.0;
        features[idx++] = (cat == Category.SPECIAL) ? 1.0 : 0.0;
        features[idx++] = (cat == Category.STATUS) ? 1.0 : 0.0;

        Type moveType = move.getType();
        features[idx++] = moveType.ordinal() / 18.0; // 18 types in Pokemon

        boolean hasSTAB = myPokemon.getCurrentType1() == moveType ||
                         (myPokemon.getCurrentType2() != null && myPokemon.getCurrentType2() == moveType);
        features[idx++] = hasSTAB ? 1.0 : 0.0;

        features[idx++] = 1.0;
        features[idx++] = 0.5; 

        // Critical hit ratio
        features[idx++] = move.getCriticalHitRatio() / 3.0; 

        String moveName = move.getName().toUpperCase();
        features[idx++] = moveName.contains("DRAIN") || moveName.contains("ABSORB") ? 1.0 : 0.0; //healing moves
        features[idx++] = moveName.contains("RECOIL") ? 1.0 : 0.0; // recoild damage
        features[idx++] = (power != null && power > 0) ? 1.0 : 0.0;

        return idx;
    }


    private int extractTypeEffectiveness(MoveView move, PokemonView myPokemon,
                                        PokemonView oppPokemon, double[] features, int startIdx) {
        int idx = startIdx;

        double effectiveness = Type.getEffectivenessModifier(move.getType(), oppPokemon.getCurrentType1());
        if (oppPokemon.getCurrentType2() != null) {
            effectiveness *= Type.getEffectivenessModifier(move.getType(), oppPokemon.getCurrentType2());
        }

        features[idx++] = effectiveness / 4.0; 
        features[idx++] = effectiveness > 1.0 ? 1.0 : 0.0; 
        features[idx++] = effectiveness < 1.0 ? 1.0 : 0.0;

        return idx;
    }

}
