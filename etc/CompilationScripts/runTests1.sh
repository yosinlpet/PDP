#!/bin/bash

ENDTIME=3600
GRAPH_NAMES=(largerGraph)

PACK_PROBAS=(0.04)

WAIT_FACTORS=(5)
AGV_SPEEDS=(0.1)

N_SEEDS=20
N_MAX_AGV=9

I=0
S=$(( ${#GRAPH_NAMES[@]}*${#WAIT_FACTORS[@]}*${#AGV_SPEEDS[@]}*${#PACK_PROBAS[@]}*$N_SEEDS )) # *$(( $N_MAX_AGV - 1 )) ))
for GRAPH in ${GRAPH_NAMES[@]}; do
	for TW in ${WAIT_FACTORS[@]};	do
		for SAGV in ${AGV_SPEEDS[@]}; do
			for PROBA in ${PACK_PROBAS[@]};	do
				for	NAGV in 9; do # $(seq 2 $N_MAX_AGV); do

					if [ $NAGV -lt 10 ]; then
						NAGV=0$NAGV
					fi

					ALL_SEEDS_FILE=".logfiles/$GRAPH/p$( echo $PROBA | sed 's/\.//g' )_nAGV$( echo $NAGV )_sAGV$( echo $SAGV | sed 's/\.//g' )_tw$( echo $TW ).dat"

					if [ -e $ALL_SEEDS_FILE ]; then
						echo File already exists
					else
						echo FANTS'	' IANTS'	' T_ANT'	' T_DEL'	' T_DIS > $ALL_SEEDS_FILE
						for SEED in $(seq 1 $N_SEEDS); do
							I=$(( 1+$I ))
							echo SIMULATION: $I / $S
							OUT_FILE="/dev/null"

							if [ $SEED -lt 10 ]; then
								SEED=0$SEED
							fi

							BOO=1
							FILE=".logfiles/$GRAPH/run_nAGV$( echo $NAGV )_sAGV$( echo $SAGV | sed 's/\.//g' )_proba$( echo $PROBA | sed 's/\.//g' )_tw$( echo $TW )_seed$( echo $SEED )"

							if [ $SEED -eq 20 ]; then
								OUT_FILE=$FILE.out
								BOO=0
							fi

							java -jar PDPProject.jar $ENDTIME $GRAPH $SEED $PROBA $TW $NAGV $SAGV > $OUT_FILE

							echo "==================================================================================="
							echo "Graph    : " $GRAPH
							echo "EndTime  : " $ENDTIME
							echo "Seed     : " $SEED
							echo "AGV_SPEED: " $SAGV
							echo "# of AGVs: " $NAGV
							echo "Pack prob: " $PROBA
							echo "TW factor: " $TW
							echo "==================================================================================="

							if [ -e $FILE.log ]; then
								FANTS=$( grep "FeasA" $FILE.log | cut -f 2 -d ':')
								IANTS=$( grep "IntA" $FILE.log | cut -f 2 -d ':')
								TOTAL_DELAY=$( grep "Total Delay:" $FILE.log | cut -f 2 -d ':')
								TOTAL_DIST=$( grep "Total Dist :" $FILE.log | cut -f 2 -d ':')
								TOTAL_ANTS=$( grep "Total Ants :" $FILE.log | cut -f 2 -d ':')
								echo $FANTS'	' $IANTS'	' $TOTAL_ANTS'	' $TOTAL_DELAY'	' $TOTAL_DIST >> $ALL_SEEDS_FILE

								if [ $BOO -eq 1 ]; then
									rm $FILE.log
								fi

							else
								echo $FILE.log does not exist
							fi
						done
					fi
				done
			done
		done
	done
done
sed -n -e '4,$p' < "stats.m" | matlab
