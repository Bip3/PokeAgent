#!/bin/bash
#$ -l h_rt=6:00:00             
#$ -pe omp 4                    
#$ -N poke_parallel_glasscannon        
#$ -o logs/poke_parallel_glasscannon.out
#$ -e logs/poke_parallel_glasscannon.err
#$ -cwd

mkdir -p logs params

javac -cp "./lib/*:." @pokePA.srcs

java -cp "./lib/*:." edu.bu.pas.pokemon.ParallelTrain \
    edu.bu.pas.pokemon.agents.RandomAgent \
    -p 2000 \
    -t 500 \
    -v 200 \
    -b 15000` \
    -r RANDOM \
    -u 2 \
    -m 128 \
    -n 1e-5 \
    -c 10 \
    -d adam \
    -g 0.99 \
    -o params/glasscannon_qFunc \
    --seed 123 \
    -j 4 \
    > logs/poke_parallel_glasscannon.log
