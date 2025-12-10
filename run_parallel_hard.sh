#!/bin/bash
#$ -l h_rt=2:00:00
#$ -pe omp 4
#$ -N poke_parallel_hard
#$ -o logs/poke_parallel_hard.out
#$ -e logs/poke_parallel_hard.err
#$ -cwd

mkdir -p logs params

# Limit threading for linear algebra libraries
export OMP_NUM_THREADS=4
export OPENBLAS_NUM_THREADS=4
export MKL_NUM_THREADS=4
export VECLIB_MAXIMUM_THREADS=4
export NUMEXPR_NUM_THREADS=4

javac -cp ".:./lib/*" @pokePA.srcs

# Optional: Set PARAMS_FILE to start from existing params
# Example: export PARAMS_FILE="params/easy_updated_qFunc.model"
LOAD_PARAMS="params/selfplay/red_training321.model"
if [ ! -z "$PARAMS_FILE" ]; then
    LOAD_PARAMS="-i $PARAMS_FILE"
    echo "Loading existing params from: $PARAMS_FILE"
fi

java -XX:ActiveProcessorCount=4 -XX:ParallelGCThreads=2 -cp ".:./lib/*" edu.bu.pas.pokemon.ParallelTrain \
    edu.bu.pas.pokemon.agents.Drac5290Agent \
    -p 2000 \
    -t 250 \
    -v 200 \
    -b 10000 \
    -r RANDOM \
    -u 2 \
    -m 128 \
    -n 1e-5 \
    -c 10 \
    -d adam \
    -g 0.99 \
    -o params/hard_updated_qFunc \
    --seed 123 \
    -j 2 \
    $LOAD_PARAMS \
    > logs/poke_parallel_hard.log
