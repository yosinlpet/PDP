function [] = stats1(x)
prefix = (".logfiles/");
graph = x
prefix2 = strcat(prefix, graph,"/");
files=dir(char(strcat(prefix2, "*5.dat")));
nseeds = 20;

nAGV = zeros(1, length(files));
Fants = zeros(nseeds, length(files));
Iants = zeros(nseeds, length(files));
Tants = zeros(nseeds, length(files));
Tdel  = zeros(nseeds, length(files));
Tdis  = zeros(nseeds, length(files));
Wpac  = zeros(nseeds, length(files));
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
		Wpac(:, i)  = data.data(:,6);

		clearvars f data;
	end
	if i > 0

		%% FIG 1: delay, distance & ants at given p
		[~, ip, ~] = unique(p);
		minVals1 = zeros(7,length(unique(p)));
		ip =  [ip; size(Tdel, 2)+1];

		p = 100.*p;
		for i = 1:length(ip)-1
			in = ip(i):ip(i+1)-1;
			minVals1(1,i) = p(in(1));

			figure(1);
			plot3(p(in), nAGV(in), log10(median(Tdel(:, in),1)), 'k.-', 'markersize', 4); hold on;
			plot3(p(in), nAGV(in), log10(mean(Tdel(:, in),1)), 'kx--', 'markersize', 4); hold on;
			[minMed,locMed] = min(log10(median(Tdel(:, in), 1)));
			[minMean,locMean] = min(log10(mean(Tdel(:, in), 1)));
			minVals1(2,i) = 10^minMed;
			minVals1(3,i) = 10^minMean;
			plot3(p(in), nAGV(locMed), minMed, 'ro', 'markersize', 5); hold on;
			plot3(p(in), nAGV(locMean), minMean, 'ro', 'markersize', 5); hold on;
			figure(2);
			plot3(p(in), nAGV(in), log10(median(Tdis(:, in),1)./nAGV(in)), 'k.-', 'markersize', 4); hold on;
			plot3(p(in), nAGV(in), log10(mean(Tdis(:, in),1)./nAGV(in)), 'kx--', 'markersize', 4); hold on;
			[minMed,locMed] = min(log10(median(Tdis(:, in), 1)./nAGV(in)));
			[minMean,locMean] = min(log10(mean(Tdis(:, in), 1)./nAGV(in)));
			minVals1(4,i) = 10^minMed;
			minVals1(5,i) = 10^minMean;
			plot3(p(in), nAGV(locMed), minMed, 'ro', 'markersize', 5); hold on;
			plot3(p(in), nAGV(locMean), minMean, 'ro', 'markersize', 5); hold on;
			figure(3);
			plot3(p(in), nAGV(in), log10(median(Tants(:, in),1)), 'k.-', 'markersize', 4); hold on;
			plot3(p(in), nAGV(in), log10(mean(Tants(:, in),1)), 'kx--', 'markersize', 4); hold on;
			[minMed,locMed] = min(log10(median(Tants(:, in), 1)));
			[minMean,locMean] = min(log10(mean(Tants(:, in), 1)));
			minVals1(6,i) = 10^minMed;
			minVals1(7,i) = 10^minMean;
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
		figname=strcat("Fig/", graph, "_", F1);
		if exist(figname, 'file') == 2
			prompt(strrcat("Do you want to override ",figname,"? if yes, type 1"));
			x = input(prompt);
			if x == 1
				print(gcf, figname, '-depsc');
		   else
			   disp(strcat(figname, "Not overriden"));
		   end
	   else
		   print(gcf, figname, '-depsc');
	   end
   end

   %% FIG 2: waitingPacks at given nAGV
   for i = 1:length(unique(nAGV))
		w = 1:length(unique(p));
		in = (w-1).*length(unique(nAGV))+i;

		figure(5);
		plot3(nAGV(in), p(in), (median(Wpac(:, in),1)./nAGV(in)), 'k.-', 'markersize', 4); hold on;
		plot3(nAGV(in), p(in), (mean(Wpac(:, in),1)./nAGV(in)), 'kx--', 'markersize', 4); hold on;
		[minMed,locMed] = min((median(Wpac(:, in), 1)./nAGV(in)));
		[minMean,locMean] = min((mean(Wpac(:, in), 1)./nAGV(in)));
		plot3(nAGV(in), p(locMed), minMed, 'ro', 'markersize', 5); hold on;
		plot3(nAGV(in), p(locMean), minMean, 'ro', 'markersize', 5); hold on;
	end
	grid on;
	yticks(unique(p))
	xticks(unique(nAGV))
	xlim([nAGV(1), nAGV(end)])
	ylim([p(1), p(end)])

	labels(["$n_{AGV}$", "$p$ $[\%]$","$n_{WP}/n_{AGV}$"],20)
	hold off;
	figname=strcat("Fig/", graph, "_nwp");
	if exist(figname, 'file') == 2
		   prompt(strrcat("Do you want to override ",figname,"? if yes, type 1"));
		   x = input(prompt);
		   if x == 1
			   print(gcf, figname, '-depsc');
		   else
			   disp(strcat(figname, "Not overriden"));
		   end
	   else
		   print(gcf, figname, '-depsc');
	   end
	   save(strcat(graph, '.txt'), 'minVals1', '-ascii');
   end
exit
