#!/bin/bash

# This script runs the Taxi PDP jar file with a bunch of user-defined parameters.
# It also post-processes output of the program (.log files) and creates Matlab plots.

# Denis Dumoulin
# Heverlee, Belgium
# June 2018

# Needs to be one for regrouping all seeds
POST_PRO=$1
POST_PRO=${POST_PRO:=0}

# Needs to be one to execute matlab scripts
MATLAB=$2
MATLAB=${MATLAB:=0}

# --------------------PARAMETERS--------------------

# Pick one of the 3 following paragraphs:
#########################################

GRAPH_NAMES=(warehouse)
N_MIN_AGV=6
N_MAX_AGV=18
STEP=2

# GRAPH_NAMES=(smallGraph)
# N_MIN_AGV=6
# N_MAX_AGV=6
# STEP=1

# GRAPH_NAMES=(testGraph)
# N_MIN_AGV=2
# N_MAX_AGV=8
# STEP=1

# GRAPH_NAMES=(largerGraph)
# N_MIN_AGV=2
# N_MAX_AGV=9
# STEP=1

#########################################

N_SEEDS=20
ENDTIME=3600

PACK_PROBAS=(0.02 0.03 0.04 0.05 0.06)

WAIT_FACTORS=(5)
AGV_SPEEDS=(0.59475)

# --------------------SIMULATION--------------------

# iterators (just for prompting)
I=0
J=0
S=$(( ${#GRAPH_NAMES[@]}*${#WAIT_FACTORS[@]}*${#AGV_SPEEDS[@]}*${#PACK_PROBAS[@]}*$N_SEEDS*$(( ($N_MAX_AGV - $N_MIN_AGV)/$STEP + 1 )) ))

for GRAPH in ${GRAPH_NAMES[@]}; do
	for TW in ${WAIT_FACTORS[@]};	do
		for SAGV in ${AGV_SPEEDS[@]}; do
			for PROBA in ${PACK_PROBAS[@]};	do
				for	NAGV in $(seq $N_MIN_AGV $STEP $N_MAX_AGV); do

					# formatting
					if [ $NAGV -lt 10 ]; then
						NAGV=0$NAGV
					fi

					# file with results from all seeds
					ALL_SEEDS_FILE=.logfiles/$GRAPH/p$( echo $PROBA | sed 's/\.//g' )_nAGV$( echo $NAGV )_sAGV$( echo $SAGV | sed 's/\.//g' )_tw$( echo $TW )

					# file for box plots
					BP_FILES=$( echo $ALL_SEEDS_FILE )_bp.dat

					ALL_SEEDS_FILE=$( echo $ALL_SEEDS_FILE ).dat

					for SEED in $(seq 1 $N_SEEDS); do
						I=$(( 1+$I ))
						OUT_FILE="/dev/null"

						# formatting
						if [ $SEED -lt 10 ]; then
							SEED=0$SEED
						fi

						FILE=.logfiles/$GRAPH/run_nAGV$( echo $NAGV )_sAGV$( echo $SAGV | sed 's/\.//g' )_proba$( echo $PROBA | sed 's/\.//g' )_tw$( echo $TW )_seed$( echo $SEED )
						if [ ! -e $FILE.log ]; then

							# only keep .out file of seed 10
							if [ $SEED -eq 10 ]; then
								OUT_FILE=$FILE.out
							fi

							echo SIMULATION: $I / $S
							java -jar PDPProject.jar false $ENDTIME $GRAPH $SEED $PROBA $TW $NAGV $SAGV > $OUT_FILE

							echo "==================================================================================="
							echo "Graph    : " $GRAPH
							echo "EndTime  : " $ENDTIME
							echo "Seed     : " $SEED
							echo "AGV_SPEED: " $SAGV
							echo "# of AGVs: " $NAGV
							echo "Pack prob: " $PROBA
							echo "TW factor: " $TW
							echo "==================================================================================="
						fi
					done

					# --------------------REGROUPING-SEEDS--------------------

					if [ $POST_PRO -eq 1 ]; then
						echo FANTS'	' IANTS'	' T_ANT'	' T_DEL'	' T_DIS'	' W_PAC > $ALL_SEEDS_FILE
						echo  L_CAR'	' L_DIS > $BP_FILES

						for SEED in $(seq 1 $N_SEEDS); do
							J=$(( 1+$J ))
							echo POST PROCESSING: $J / $S

							# formatting
							if [ $SEED -lt 10 ]; then
								SEED=0$SEED
							fi

							FILE=.logfiles/$GRAPH/run_nAGV$( echo $NAGV )_sAGV$( echo $SAGV | sed 's/\.//g' )_proba$( echo $PROBA | sed 's/\.//g' )_tw$( echo $TW )_seed$( echo $SEED ).log

							if [ -e $FILE ]; then
								LCAR=$( grep "AGV_Car" $FILE | cut -f 2 -d ':' )
								LDIS=$( grep "AGV_Tra" $FILE | cut -f 2 -d ':' )
								LDIS=$( echo import numpy$'\n'"y=numpy.array($LDIS)"$'\n'"y=y[numpy.argsort($LCAR)]"$'\n'print y | python )
								LCAR=$( echo import numpy$'\n'"y=numpy.array($LCAR)"$'\n'"y=y[numpy.argsort($LCAR)]"$'\n'print y | python )
								FANTS=$( grep "FeasA" $FILE | cut -f 2 -d ':')
								IANTS=$( grep "IntA" $FILE | cut -f 2 -d ':')
								TOTAL_DELAY=$( grep "Total Delay" $FILE | cut -f 2 -d ':')
								WAITING_PACKS=$( grep "Waiting Packs:" $FILE | cut -f 2 -d ':')
								TOTAL_DIST=$( grep "Total Dist" $FILE | cut -f 2 -d ':')
								TOTAL_ANTS=$( grep "Total Ants" $FILE | cut -f 2 -d ':')

								echo $FANTS'	' $IANTS'	' $TOTAL_ANTS' 	' $TOTAL_DELAY' 	' $TOTAL_DIST'	'$WAITING_PACKS >> $ALL_SEEDS_FILE
								echo $LCAR'	' $LDIS >> $BP_FILES
							else
								echo $FILE does not exist!
							fi
						done
					fi
				done
			done
		done
	done
	# --------------------MATLAB-PLOTS-GENERATION--------------------
	if [ $MATLAB -eq 1 ]; then
		matlab -nodisplay -r "stats1(\"$GRAPH\")"
		./tabularizer.sh $GRAPH
		matlab -nodisplay -r "stats2(\"$GRAPH\")"
	fi
done
