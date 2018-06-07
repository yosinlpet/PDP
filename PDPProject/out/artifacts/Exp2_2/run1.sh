#!/bin/bash

VISUAL=true 			#visual mode?
PDP_MODEL=false			#visual for pdp model renderer
# GRAPH=largerGraph		#name of the .dot file
# GRAPH=testGraph		#name of the .dot file
GRAPH=warehouse		#name of the .dot file
# GRAPH=smallGraph		#name of the .dot file
NAGV=06					#number of AGVs
SAGV=0.2				#speed of AGVs
PROBA=0.02				#proba of Orders
TW=5					#order is late if TW times time for going through shortest path has past
END=3600				#end time of the simulation
SEED=09					#seed for random generator (however, rinsim looks quite random in itself...)

java -jar PDPProject.jar $VISUAL $END $GRAPH $SEED $PROBA $TW $NAGV $SAGV $PDP_MODEL > /dev/null 
