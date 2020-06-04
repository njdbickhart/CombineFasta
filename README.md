# CombineFasta
---

A "swiss-army knife" for resectioning and joining fasta files. 

**Requirements:**
* Java jdk version 1.8
* This git repository
* A decent (>16Gb) amount of RAM

Technically, this program works on all operating systems; however, it has only been tested on Linux.

## Installation:

Since I am updating and bug-fixing this constantly, it is best to clone the repository and run the packaged JAR I have compiled in the "store" directory. In order to clone the repository, please type the following command:

```bash
git clone https://njdbickhart/CombineFasta
```

This should create a new directory called "CombineFasta" in your current working directory. In order to invoke the help menu of the program, just type this command:

```bash
java -jar CombineFasta/store/CombineFasta.jar
```

This should give you a general help menu. In order to invoke a specific module (and more are on the way!(TM)) you just type a command like so:

```bash
java -jar CombineFasta/store/CombineFasta.jar order
```

Each mode has its own separate help menu to assist you in remembering how to run the program.

## Modes:

#### order mode

This mode allows you to join two sections of a fasta file rapidly. A good use case for this would be in generating new scaffolds from multiple contigs, resectioning old scafolds after splitting them, or even just reverse orienting whole chromosomes.

General usage:

```bash
java -jar ~/binaries/CombineFasta/store/CombineFasta.jar order
	CombineFasta order:
	Usage: java -jar CombineFasta.jar order -i [tab delim input] -o [output fasta] -p [padding bases] -n [fasta name]
        -i      Input single entry fasta files in tab delimited format with orientations in second column
        -o      Output fasta file name
        -p      Number of N bases to pad fasta entries
	-n	Name of merged fasta sequence [default: "merged"]
```

Example usage (using samtools to resection a fasta):

```bash
samtools faidx my_old_fasta.fa oldscaffold:1-1000 > new_scaffold_seg1.fa
samtools faidx my_old_fasta.fa oldscaffold:1000-2000 > new_scaffold_seg2.fa

echo -e "new_scaffold_seg1.fa\t+" > contig_order.list
echo -e "new_scaffold_seg2.fa\t-" >> contig_order.list

java -jar ~/binaries/CombineFasta/store/CombineFasta.jar order -i contig_order.list -o my_new_fasta.fa -p 100 -n "newChr"
```

#### agp2fasta mode

Contrary to the name, this mode allows both bed and agp file formats to be used. The output product is a new scaffold fasta derived from the instructions in the agp or bed file. 

General usage:

```bash
java -Xmx14g -jar store/CombineFasta.jar agp2fasta
CombineFasta agp2fasta:
Usage: java -jar CombineFasta.jar agp2fasta -f [original fasta] -a [agp file] -o [output fasta name]
NOTE: select EITHER -b or -a for input! AGP (-a) input is preferentially used
        -f      The input fasta to be subsectioned for incorporation into the AGP file
        -b      A bed file [1-3 fasta file coordinates, 4 final scaffold name, 5 order integer, 6 orientation {+/-}]
        -a      The agp file for ordering fasta subsections
        -i      (Used only with Bed format input) The length of gap sequence [100]
        -o      The full output name of the resultant fasta file

```

As noted in the usage statement, you should either use "-b" or "-a" for your input instruction file. The AGP file should follow NCBI file format specifications. The bed file must have the following columns:

1. Chromosome in original fasta
2. Start position in original fasta
3. End position in original fasta
4. Final Scaffold name
5. The order in the scaffold (whole numbers from 1 to N, indicating how to organize the segments)
6. The orientation of the segment from the original fasta

####NOTE: Bed files do not need gap entries -- these are added automatically after each segment

Here is an example use:

```bash
# BED INSTRUCTIONS
# plan.bed contents:
# contig_2	1	200	scaffold_1	1	+
# contig_3	1	200	scaffold_1	3	+
# contig_1	1	200	scaffold_1	2	-
# contig_3	101	200	scaffold_2	1	-
# contig_1	51	100	scaffold_2	2	+

java -Xmx14g -jar CombineFasta.jar agp2fasta -b plan.bed -i 100 -f original.fasta -o output.fasta

# AGP INSTRUCTIONS
# plan.agp contents:
# scaffold_1	1	200	1	W	contig_2	1	200	+
# scaffold_1	201	300	2	N	100	scaffold	yes	na
# scaffold_1	301	500	3	W	contig_1	1	200	-
# scaffold_1	501	600	4	N	100	scaffold	yes	na
# scaffold_1	601	800	5	W	contig_3	1	200	+

java -Xmx14g -jar CombineFasta.jar agp2fasta -a plan.agp -f original.fasta -o output.fasta
```