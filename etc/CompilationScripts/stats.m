#!/bin/bash
sed -n -e '4,$p' < "$0" | matlab
exit $?

clear; close all;
prefix = (".logfiles/");
graphs = dir(char(strcat(prefix,"*Graph")));
for dirs = graphs'
	graph = dirs.name
	prefix2 = strcat(prefix, graph,"/");
	files=dir(char(strcat(prefix2, "*.dat")));
	nseeds = 20;

	nAGV = zeros(1, length(files));
	Fants = zeros(nseeds, length(files));
	Iants = zeros(nseeds, length(files));
	Tants = zeros(nseeds, length(files));
	Tdel  = zeros(nseeds, length(files));
	Tdis  = zeros(nseeds, length(files));
	p = nAGV;
	i = 0;

	for file = files'
		i = i + 1;

		f    = file.name;
		data = importdata(char(strcat(prefix2, f)));

		nAGV(i) = str2double(f(10:11));
		p(i)    = str2double(f(2:4))/100.;

		Fants(:, i) = data.data(:,1);
		Iants(:, i) = data.data(:,2);
		Tants(:, i) = data.data(:,3);
		Tdel(:, i)  = data.data(:,4);
		Tdis(:, i)  = data.data(:,5);

		clearvars f data;
  end

  [~, ip, ~] = unique(p);
  ip =  [ip; size(Tdel, 2)+1];
  figure();
  p = 100.*p;
  for i = 1:length(ip)-1
	  in = ip(i):ip(i+1)-1;
	  figure(1);
	  plot3(p(in), nAGV(in), log10(median(Tdel(:, in),1)), 'k.-', 'markersize', 4); hold on;
	  plot3(p(in), nAGV(in), log10(mean(Tdel(:, in),1)), 'kx--', 'markersize', 4); hold on;
		[minMed,locMed] = min(log10(median(Tdel(:, in), 1)));
		[minMean,locMean] = min(log10(mean(Tdel(:, in), 1)));
		plot3(p(in), nAGV(locMed), minMed, 'ro', 'markersize', 5); hold on;
		plot3(p(in), nAGV(locMean), minMean, 'ro', 'markersize', 5); hold on;
	  figure(2);
	  plot3(p(in), nAGV(in), log10(median(Tdis(:, in),1)./nAGV(in)), 'k.-', 'markersize', 4); hold on;
	  plot3(p(in), nAGV(in), log10(mean(Tdis(:, in),1)./nAGV(in)), 'kx--', 'markersize', 4); hold on;
		[minMed,locMed] = min(log10(median(Tdis(:, in), 1)./nAGV(in)));
		[minMean,locMean] = min(log10(mean(Tdis(:, in), 1)./nAGV(in)));
		plot3(p(in), nAGV(locMed), minMed, 'ro', 'markersize', 5); hold on;
		plot3(p(in), nAGV(locMean), minMean, 'ro', 'markersize', 5); hold on;
	  figure(3);
	  plot3(p(in), nAGV(in), log10(median(Tants(:, in),1)), 'k.-', 'markersize', 4); hold on;
	  plot3(p(in), nAGV(in), log10(mean(Tants(:, in),1)), 'kx--', 'markersize', 4); hold on;
		[minMed,locMed] = min(log10(median(Tants(:, in), 1)));
		[minMean,locMean] = min(log10(mean(Tants(:, in), 1)));
		plot3(p(in), nAGV(locMed), minMed, 'ro', 'markersize', 5); hold on;
		plot3(p(in), nAGV(locMean), minMean, 'ro', 'markersize', 5); hold on;
end
for i = 1:3
	figure(i);
	if i == 1
		F1 = "Delay";
		F = strcat("$log_{10}$(", F1,") $[s]$");
		view(-37.5,40)
	elseif i == 2
		F1 = "Dist";
		F = strcat("$log_{10}(\overline{", F1, "})$ $[m]$");
	else
		F1 = "N";
		F = strcat("$log_{10}(",F1, "_{ants}$)");
	end
	grid on;
	yticks(unique(nAGV))
	xticks(unique(p))
	xlim([p(1), p(end)])
	ylim([nAGV(1), nAGV(end)])

	labels(["$p$ $[\%]$","$n_{AGV}$",F],20)
	hold off;
	print(gcf, strcat("Fig/", graph, "_", F1), '-depsc');
end
end
