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

More usage cases to come!