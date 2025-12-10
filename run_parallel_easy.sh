#!/bin/bash
#$ -l h_rt=12:00:00
#$ -pe omp 4
#$ -N poke_parallel_easy
#$ -o logs/reduced/poke_parallel_easy.out
#$ -e logs/reduced/poke_parallel_easy.err
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
    -u 10 \
    -m 128 \
    -n 5e-4 \
    -c 10 \
    -d adam \
    -g 0.99 \
    -o params/reduced/easy_updated_qFunc \
    --seed 123 \
    -j 4 \
    > logs/reduced/poke_parallel_easy.log
