#!/bin/bash
#$ -l h_rt=24:00:00
#$ -pe omp 4
#$ -N poke_selfplay_red
#$ -o logs/poke_selfplay_red.out
#$ -e logs/poke_selfplay_red.err
#$ -cwd

mkdir -p logs params

# Limit threading for linear algebra libraries
export OMP_NUM_THREADS=4
export OPENBLAS_NUM_THREADS=4
export MKL_NUM_THREADS=4
export VECLIB_MAXIMUM_THREADS=4
export NUMEXPR_NUM_THREADS=4

javac -cp "./lib/*:." @pokePA.srcs

# Load your best model as starting point (the one that got 100% on autograder)
BEST_MODEL="params/Newmodels/easy_resume95/easy_qfunc_resume95.model"

java -Xmx8g -XX:ActiveProcessorCount=4 -XX:ParallelGCThreads=2 -cp "./lib/*:." edu.bu.pas.pokemon.ParallelTrain \
    src.pas.pokemon.agents.PolicyAgent \
    src.pas.pokemon.agents.PolicyAgent \
    -i $BEST_MODEL \
    -p 2000 \
    -t 500 \
    -v 200 \
    -b 15000 \
    -r RANDOM \
    -u 10 \
    -m 128 \
    -n 1e-4 \
    -c 10 \
    -d adam \
    -g 0.99 \
    -o params/selfplay/red_training \
    --seed 456 \
    -j 4 \
    > logs/poke_selfplay_red.log
