#!/bin/bash

# takes a .txt table and creates a .tex table

GRAPH=$1

# Computes the number of columns of the file
NUM_COLS=$( awk '{print NF}' $GRAPH.txt | sort -nu | tail -n 1 )
col=c
for a in $(seq 1 $(($NUM_COLS-1))); do
	col+=c
done

echo \\documentclass\[12pt, a5paper\]\{standalone\} > $GRAPH.tex
echo '	'\\begin\{document\} >> $GRAPH.tex
echo '		'\\begin\{tabular\}\{\|c\|$col\|\} >> $GRAPH.tex
echo '		'\\hline >> $GRAPH.tex
HDRS=("\$p\$ \$[\\%]\$" "\$M_{Delay}\$ \$[s]\$" "\$\\overline{Delay}\$ \$[s]\$" "\$M_{Dist}\$ \$[m]\$" "\$\\overline{Dist}\$ \$[m]\$" "\$M_{n_{Ants}}\$" "\$\\overline{n_{Ants}}\$")
i=0
while read p; do
	echo '		'${HDRS[$i]} $( printf ' %.2f' $p | sed 's/\ /\ \& /g') \\\\ >> $GRAPH.tex
	i=$(($i+1))
	if [ $i -eq 1 ]; then
		echo \\hline >> $GRAPH.tex
	fi
  done < $GRAPH.txt
echo '		'\\hline >> $GRAPH.tex
echo '	'\\end\{tabular\} >> $GRAPH.tex
echo \\end\{document\} >> $GRAPH.tex

latex $GRAPH.tex
dvips -o ./Fig/$GRAPH.eps $GRAPH.dvi
rm $GRAPH.dvi $GRAPH.tex $GRAPH.aux $GRAPH.log $GRAPH.txt
