#!/bin/bash

ENDTIME=3600
GRAPH_NAMES=(largerGraph)

PACK_PROBAS=(0.02 0.05 0.07)

WAIT_FACTORS=(5)
AGV_SPEEDS=(0.1)

for GRAPH in ${GRAPH_NAMES[@]}
do
	for TW in ${WAIT_FACTORS[@]}
	do
		for SAGV in ${AGV_SPEEDS[@]}
		do
			for PROBA in ${PACK_PROBAS[@]}
			do
				for	NAGV in $(seq 2 8) 
				do
					if [ $NAGV -lt 10 ]; then
						NAGV=0$NAGV
						echo $NAGV
					fi

					ALL_SEEDS_FILE=".logfiles/$GRAPH/nAGV$( echo $NAGV )_sAGV$( echo $SAGV | sed 's/\.//g' )_proba$( echo $PROBA | sed 's/\.//g' )_tw$( echo $TW ).dat"
					echo FANTS'	' IANTS'	' T_ANT'	' T_DEL'	' T_DIS > $ALL_SEEDS_FILE
					for SEED in $(seq 1 20)
					do

						if [ $SEED -lt 10 ]; then
							SEED=0$SEED
						fi

						FILE=".logfiles/$GRAPH/run_nAGV$( echo $NAGV )_sAGV$( echo $SAGV | sed 's/\.//g' )_proba$( echo $PROBA | sed 's/\.//g' )_tw$( echo $TW )_seed$( echo $SEED )"
						java -jar PDPProject.jar $ENDTIME $GRAPH $SEED $PROBA $TW $NAGV $SAGV> $FILE.out
						echo "============================================================"
						echo "Graph    : " $GRAPH
						echo "EndTime  : " $ENDTIME
						echo "Seed     : " $SEED
						echo "AGV_SPEED: " $SAGV
						echo "# of AGVs: " $NAGV
						echo "Pack prob: " $PROBA
						echo "TW factor: " $TW
						echo "============================================================"
						if [ -e $FILE.log ]
						then
							FANTS=$( grep "FeasA" $FILE.log | cut -f 2 -d ':') 
							IANTS=$( grep "IntA" $FILE.log | cut -f 2 -d ':') 
							TOTAL_DELAY=$( grep "Total Delay:" $FILE.log | cut -f 2 -d ':') 
							TOTAL_DIST=$( grep "Total Dist :" $FILE.log | cut -f 2 -d ':')
							TOTAL_ANTS=$( grep "Total Ants :" $FILE.log | cut -f 2 -d ':')
							echo $FANTS'	' $IANTS'	' $TOTAL_ANTS'	' $TOTAL_DELAY'	' $TOTAL_DIST >> $ALL_SEEDS_FILE
						else
							echo $FILE.log does not exist
						fi
					done
				done
			done
		done
	done
done
