#!/bin/bash
#$ -l h_rt=1:00:00             
#$ -pe omp 4                    
#$ -N poke_parallel_easy        
#$ -o logs/poke_parallel_easy.out
#$ -e logs/poke_parallel_easy.err
#$ -cwd

mkdir -p logs params

javac -cp "./lib/*:." @pokePA.srcs

java -cp "./lib/*:." ParallelTrain \
    src.pas.pokemon.agents.RandomAgent \
    src.pas.pokemon.agents.GymBrockAgent \
    -p 2000 \
    -t 500 \
    -v 200 \
    -b 50000 \
    -r RANDOM \
    -u 2 \
    -m 128 \
    -n 1e-5 \
    -c 10 \
    -d adam \
    -g 0.99 \
    -o /projectnb/cs440/students/aking03/pokemon/params/easy_qFunc \
    --seed 123 \
    -j 4 \
    > logs/poke_parallel_easy.log
