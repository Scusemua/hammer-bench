EXTRA_JAVAPATH = :/home/ben/repos/hops/hadoop-dist/target/hadoop-3.2.0.2-RC0/share/hadoop/hdfs/lib/*:/home/ben/repos/hops/hadoop-dist/target/hadoop-3.2.0.2-RC0/share/hadoop/common/lib/*

bench:
	java -cp "hop-experiments-1.0-SNAPSHOT-jar-with-dependencies.jar$(EXTRA_JAVAPATH)" io.hops.experiments.controller.Slave 1>/dev/null 2>&1 &