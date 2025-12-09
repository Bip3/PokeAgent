#!/bin/bash
#$ -l h_rt=1:00:00
#$ -pe omp 4
#$ -N poke_parallel_easy
#$ -o logs/poke_parallel_easy.out
#$ -e logs/poke_parallel_easy.err
#$ -cwd

mkdir -p logs params

# Limit threading for linear algebra libraries
export OMP_NUM_THREADS=4
export OPENBLAS_NUM_THREADS=4
export MKL_NUM_THREADS=4
export VECLIB_MAXIMUM_THREADS=4
export NUMEXPR_NUM_THREADS=4

javac -cp "./lib/*:." @pokePA.srcs

java -XX:ActiveProcessorCount=4 -XX:ParallelGCThreads=2 -cp "./lib/*:." edu.bu.pas.pokemon.ParallelTrain \
    edu.bu.pas.pokemon.agents.RandomAgent \
    edu.bu.pas.pokemon.agents.AggroAgent \
    -p 2000 \
    -t 500 \
    -v 200 \
    -b 15000 \
    -r RANDOM \
    -u 2 \
    -m 128 \
    -n 1e-5 \
    -c 10 \
    -d adam \
    -g 0.99 \
    -o params/easy_updated_qFunc \
    --seed 123 \
    -j 4 \
    > logs/poke_parallel_easy.log
