EXTRA_JAVAPATH = :/home/ben/repos/hops/hadoop-dist/target/hadoop-3.2.0.2-RC0/share/hadoop/hdfs/lib/*:/home/ben/repos/hops/hadoop-dist/target/hadoop-3.2.0.2-RC0/share/hadoop/common/lib/*

mvn-local-install:
	mvn install:install-file -Dfile=lib/hadoop-hdfs.jar -DgroupId=io.hops -DartifactId=hadoop-hdfs -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
	mvn install:install-file -Dfile=lib/hadoop-hdfs-client-3.2.0.2-RC0.jar -DgroupId=io.hops -DartifactId=hadoop-hdfs-client -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
	mvn install:install-file -Dfile=lib/hadoop-common.jar -DgroupId=io.hops -DartifactId=hadoop-common -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
	
mvn-client-install:
	mvn install:install-file -Dfile=/home/ben/repos/hops/hadoop-hdfs-project/hadoop-hdfs/target/hadoop-hdfs-3.2.0.2-RC0.jar -DgroupId=io.hops -DartifactId=hadoop-hdfs -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
	mvn install:install-file -Dfile=/home/ben/repos/hops/hadoop-hdfs-project/hadoop-hdfs-client/target/hadoop-hdfs-client-3.2.0.2-RC0.jar -DgroupId=io.hops -DartifactId=hadoop-hdfs-client -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
	mvn install:install-file -Dfile=/home/ben/repos/hops/hadoop-common-project/hadoop-common/target/hadoop-common-3.2.0.2-RC0.jar -DgroupId=io.hops -DartifactId=hadoop-common -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true

build: mvn-client-install
	mvn verify

install-slave: build
	cp -f target/hop-experiments-1.0-SNAPSHOT-jar-with-dependencies.jar slave.target/
	cp -f slave.properties slave.target/

bench:
	java -Dsun.io.serialization.extendedDebugInfo=true -Xmx16g -Xms16g -XX:+UseConcMarkSweepGC -XX:+UnlockDiagnosticVMOptions -XX:ParGCCardsPerStrideChunk=4096 -XX:+CMSScavengeBeforeRemark -XX:MaxGCPauseMillis=350 -XX:MaxTenuringThreshold=2 -XX:MaxNewSize=8000m -XX:+CMSClassUnloadingEnabled -XX:+ScavengeBeforeFullGC -verbose:gc -XX:+PrintGCTimeStamps -XX:+PrintGCDetails -cp target/hop-experiments-1.0-SNAPSHOT-jar-with-dependencies.jar io.hops.experiments.controller.Master

start-slave:
	cd slave.target && java -Dsun.io.serialization.extendedDebugInfo=true -Xmx48g -Xms48g -XX:+UseConcMarkSweepGC -XX:+UnlockDiagnosticVMOptions -XX:ParGCCardsPerStrideChunk=4096 -XX:+CMSScavengeBeforeRemark -XX:MaxGCPauseMillis=350 -XX:MaxTenuringThreshold=2 -XX:MaxNewSize=32000m -XX:+CMSClassUnloadingEnabled -XX:+ScavengeBeforeFullGC -verbose:gc -XX:+PrintGCTimeStamps -XX:+PrintGCDetails -cp "hop-experiments-1.0-SNAPSHOT-jar-with-dependencies.jar$(EXTRA_JAVAPATH)" io.hops.experiments.controller.Slave