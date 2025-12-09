package src.pas.pokemon.agents;


// SYSTEM IMPORTS
import net.sourceforge.argparse4j.inf.Namespace;

import edu.bu.pas.pokemon.agents.NeuralQAgent;
import edu.bu.pas.pokemon.agents.senses.SensorArray;
import edu.bu.pas.pokemon.core.Battle.BattleView;
import edu.bu.pas.pokemon.core.Move.MoveView;
import edu.bu.pas.pokemon.core.Pokemon.PokemonView;
import edu.bu.pas.pokemon.core.Team.TeamView;
import edu.bu.pas.pokemon.linalg.Matrix;
import edu.bu.pas.pokemon.nn.Model;
import edu.bu.pas.pokemon.nn.models.Sequential;
import edu.bu.pas.pokemon.nn.layers.Dense; // fully connected layer
import edu.bu.pas.pokemon.nn.layers.ReLU;  // some activations (below too)
import edu.bu.pas.pokemon.nn.layers.Tanh;
import edu.bu.pas.pokemon.nn.layers.Sigmoid;

import java.util.List;
import java.util.Random;

import edu.bu.pas.pokemon.core.enums.Stat;


// JAVA PROJECT IMPORTS
import src.pas.pokemon.senses.CustomSensorArray;


public class PolicyAgent
    extends NeuralQAgent
{
    // Epsilon-greedy exploration parameters
    private double epsilon;
    private static final double EPSILON_START = 1.0;  // Start with 100% exploration
    private static final double EPSILON_END = 0.05;   // End with 5% exploration
    private static final double EPSILON_DECAY = 0.9995; // Decay rate per game

    // Statistics tracking
    private int gamesPlayed;
    private int gamesWon;
    private int gamesLost;
    private double totalReward;

    // Random number generator
    private Random random;

    public PolicyAgent()
    {
        super();
        this.epsilon = EPSILON_START;
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.gamesLost = 0;
        this.totalReward = 0.0;
        this.random = new Random();
    }

    public void initializeSenses(Namespace args)
    {
        SensorArray modelSenses = new CustomSensorArray();
        this.setSensorArray(modelSenses);
    }


    @Override
    public void initialize(Namespace args)
    {
        // make sure you call this, this will call your initModel() and set a field
        // AND if the command line argument "inFile" is present will attempt to set
        // your model with the contents of that file.
        super.initialize(args);

        // what senses will your neural network have?
        this.initializeSenses(args);

        // do what you want just don't expect custom command line options to be available
        // when I'm testing your code
    }

    @Override
    public Model initModel() {
        Sequential qFunction = new Sequential();

        // Input layer size matches CustomSensorArray.NUM_FEATURES (70)
        int numFeatures = CustomSensorArray.NUM_FEATURES;

        // Deeper network for complex Pokemon battle decisions
        qFunction.add(new Dense(numFeatures, 256));
        qFunction.add(new ReLU());

        qFunction.add(new Dense(256, 128));
        qFunction.add(new ReLU());

        qFunction.add(new Dense(128, 64));
        qFunction.add(new ReLU());

        qFunction.add(new Dense(64, 32));
        qFunction.add(new ReLU());

        qFunction.add(new Dense(32, 1));  // scalar Q(s,a) output

        return qFunction;
    }

    @Override
    public Integer chooseNextPokemon(BattleView view)
    {
        TeamView myTeam = this.getMyTeamView(view);
        TeamView oppTeam = this.getOpponentTeamView(view);
        PokemonView oppPokemon = oppTeam.getActivePokemonView();

        int bestIdx = -1;
        double bestScore = Double.NEGATIVE_INFINITY;

        // Choose Pokemon with best combination of HP and type advantage
        for(int idx = 0; idx < myTeam.size(); ++idx)
        {
            PokemonView pokemon = myTeam.getPokemonView(idx);

            if(!pokemon.hasFainted())
            {
                // Calculate a score based on HP percentage and type matchup
                int currentHP = pokemon.getCurrentStat(Stat.HP);
                int maxHP = pokemon.getInitialStat(Stat.HP);
                double hpScore = (double) currentHP / maxHP;

                // Simple type advantage calculation
                double typeScore = 0.0;
                if (oppPokemon != null) {
                    // Check if our Pokemon's type is strong against opponent
                    // This is a simplified heuristic
                    if (pokemon.getCurrentType1() != null) {
                        typeScore += calculateSimpleTypeAdvantage(pokemon.getCurrentType1(), oppPokemon);
                    }
                    if (pokemon.getCurrentType2() != null) {
                        typeScore += calculateSimpleTypeAdvantage(pokemon.getCurrentType2(), oppPokemon);
                    }
                }

                // Combined score: prioritize HP but consider type advantage
                double score = hpScore * 2.0 + typeScore;

                if (score > bestScore) {
                    bestScore = score;
                    bestIdx = idx;
                }
            }
        }

        return bestIdx != -1 ? bestIdx : null;
    }

    /**
     * Simple heuristic for type advantage
     */
    private double calculateSimpleTypeAdvantage(edu.bu.pas.pokemon.core.enums.Type myType, PokemonView oppPokemon) {
        double advantage = 0.0;

        // Check effectiveness against opponent's types
        advantage += edu.bu.pas.pokemon.core.enums.Type.getEffectivenessModifier(myType, oppPokemon.getCurrentType1());
        if (oppPokemon.getCurrentType2() != null) {
            advantage += edu.bu.pas.pokemon.core.enums.Type.getEffectivenessModifier(myType, oppPokemon.getCurrentType2());
        }

        return advantage - 1.0; // Normalize around 0
    }

    @Override
    public MoveView getMove(BattleView view)
    {
        // During training: epsilon-greedy exploration
        // During evaluation: always exploit (epsilon should be set to 0)
        if (this.random.nextDouble() < this.epsilon) {
            // EXPLORATION: Choose random legal move
            List<MoveView> potentialMoves = this.getPotentialMoves(view);
            if (potentialMoves != null && !potentialMoves.isEmpty()) {
                int randomIdx = this.random.nextInt(potentialMoves.size());
                return potentialMoves.get(randomIdx);
            }
        }

        // EXPLOITATION: Choose best move according to Q-function
        return this.argmax(view);
    }

    @Override
    public void afterGameEnds(BattleView view)
    {
        // Update game statistics
        this.gamesPlayed++;

        // Check if we won by checking if opponent team is all fainted
        TeamView myTeam = this.getMyTeamView(view);
        TeamView oppTeam = this.getOpponentTeamView(view);

        boolean oppAllFainted = true;
        for (int i = 0; i < oppTeam.size(); i++) {
            if (!oppTeam.getPokemonView(i).hasFainted()) {
                oppAllFainted = false;
                break;
            }
        }

        if (oppAllFainted) {
            this.gamesWon++;
        } else {
            this.gamesLost++;
        }

        // Decay epsilon for next game (only during training)
        if (this.epsilon > EPSILON_END) {
            this.epsilon *= EPSILON_DECAY;
            // Ensure epsilon doesn't go below minimum
            if (this.epsilon < EPSILON_END) {
                this.epsilon = EPSILON_END;
            }
        }

        // Print statistics periodically (will be disabled during training on SCC)
        if (this.gamesPlayed % 100 == 0) {
            double winRate = (double) this.gamesWon / this.gamesPlayed;
            System.out.println(String.format(
                "Games: %d | Wins: %d | Losses: %d | Win Rate: %.3f | Epsilon: %.4f",
                this.gamesPlayed, this.gamesWon, this.gamesLost, winRate, this.epsilon
            ));
        }
    }

    /**
     * Set epsilon manually (useful for evaluation mode)
     */
    public void setEpsilon(double epsilon) {
        this.epsilon = Math.max(0.0, Math.min(1.0, epsilon));
    }

    /**
     * Get current epsilon value
     */
    public double getEpsilon() {
        return this.epsilon;
    }

    /**
     * Reset statistics
     */
    public void resetStatistics() {
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.gamesLost = 0;
        this.totalReward = 0.0;
    }

}

