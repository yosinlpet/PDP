function [] = stats2(graph)
prefix = (".logfiles/");
prefix2 = strcat(prefix, graph,"/");
files=dir(char(strcat(prefix2, "*bp.dat")));
nseeds = 20;

nAGV = zeros(1, length(files));
Fants = zeros(nseeds, length(files));
Iants = zeros(nseeds, length(files));
Tants = zeros(nseeds, length(files));
Tdel  = zeros(nseeds, length(files));
Tdis  = zeros(nseeds, length(files));
p = nAGV;
i = 0;
delimiter = {'\t',' ','[',']', '[ '};
startRow = 2;
stopRow = 20;
IDs= ["Truck-000","Truck-001","Truck-002","Truck-003","Truck-004","Truck-005","Truck-006","Truck-007","Truck-008","Truck-009","Truck-010","Truck-011","Truck-012","Truck-013","Truck-014","Truck-015","Truck-016","Truck-017"];
for file = files'
		i = i + 1;

		f       = file.name;
		nAGV(i) = str2double(f(10:11));
		p(i)    = str2double(f(2:4))/100.;
		Ldel = zeros(nseeds, nAGV(i));
		Ldis = Ldel;

		f2 = "%f";
		for zz = 2:nAGV(i)
			f2 = strcat(f2, "%f");
		end

		fid        = fopen(char(strcat(prefix2, f)), 'r');
		formatSpec = strcat("%*s",f2,"%*s%*s%*s",f2,"%*s%[^\n\r]");
		data = textscan(fid, formatSpec, stopRow, 'Delimiter', delimiter, 'TextType', 'string', 'HeaderLines' ,startRow-1, 'ReturnOnError', false, 'EndOfLine', '\r\n');
		fclose(fid);

		for jj = 0:nAGV(i)-1
			Ldel(:,jj+1) = data{1+jj};
			Ldis(:,jj+1) = data{1+nAGV(i)+jj};
		end


		hFig = figure(4);
		set(hFig, 'Position', [120 70 1600 900])
		subplot(2,1,1)
		boxplot(Ldel);
		labels(["AGV IDs","$n_{cp}$"],20);
		title(strcat("$p=", string(100*p(i)), "[\%]$"), 'interpreter', 'latex', 'fontsize', 20);
		xticklabels(IDs(1:nAGV(i)));
		subplot(2,1,2)
		boxplot(Ldis);
		labels(["AGV IDs","Dist$_{AGV}$ $[m]$"],20);
		xticklabels(IDs(1:nAGV(i)));
		print(gcf,strcat("Fig/", graph, "_bp_p",string(100*p(i)),"_nAGV",string(nAGV(i))),'-depsc');
		hold off;
	end
	exit
