build:
	mvn install:install-file -Dfile=lib/hadoop-hdfs.jar -DgroupId=io.hops -DartifactId=hadoop-hdfs -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
	mvn install:install-file -Dfile=lib/hadoop-hdfs-client-3.2.0.2-RC0.jar -DgroupId=io.hops -DartifactId=hadoop-hdfs-client -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
	mvn install:install-file -Dfile=lib/hadoop-common.jar -DgroupId=io.hops -DartifactId=hadoop-common -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
	mvn verify

bench:
	java -cp target/hop-experiments-1.0-SNAPSHOT-jar-with-dependencies.jar io.hops.experiments.controller.Master