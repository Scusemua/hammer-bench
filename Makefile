EXTRA_JAVAPATH = :/home/ben/repos/hops/hadoop-dist/target/hadoop-3.2.0.3-SNAPSHOT/share/hadoop/hdfs/lib/*:/home/ben/repos/hops/hadoop-dist/target/hadoop-3.2.0-SNAPSHOT/share/hadoop/common/lib/*:/home/ben/repos/hops/hadoop-hdfs-project/hadoop-hdfs-client/target/hadoop-hdfs-client-3.2.0.3-SNAPSHOT.jar:/home/ben/repos/hops/hops-leader-election/target/hops-leader-election-3.2.0.3-SNAPSHOT.jar:/home/ben/openwhisk-runtime-java/core/java8/libs/*:/home/ben/repos/hops/hadoop-hdfs-project/hadoop-hdfs/target/hadoop-hdfs-3.2.0.3-SNAPSHOT.jar:/home/ben/repos/hops/hadoop-common-project/hadoop-common/target/hadoop-common-3.2.0.3-SNAPSHOT.jar

mvn-local-install:
	mvn install:install-file -Dfile=lib/hadoop-hdfs.jar -DgroupId=io.hops -DartifactId=hadoop-hdfs -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
	mvn install:install-file -Dfile=lib/hadoop-hdfs-client.jar -DgroupId=io.hops -DartifactId=hadoop-hdfs-client -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
	mvn install:install-file -Dfile=lib/hadoop-common.jar -DgroupId=io.hops -DartifactId=hadoop-common -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
	
mvn-client-install:
	mvn install:install-file -Dfile=/home/ben/repos/hops/hadoop-hdfs-project/hadoop-hdfs/target/hadoop-hdfs-3.2.0.3-SNAPSHOT.jar -DgroupId=io.hops -DartifactId=hadoop-hdfs -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
	mvn install:install-file -Dfile=/home/ben/repos/hops/hadoop-hdfs-project/hadoop-hdfs-client/target/hadoop-hdfs-client-3.2.0.3-SNAPSHOT.jar -DgroupId=io.hops -DartifactId=hadoop-hdfs-client -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
	mvn install:install-file -Dfile=/home/ubuntu/repos/hops/hadoop-common-project/hadoop-common/target/hadoop-common-3.2.0.3-SNAPSHOT.jar -DgroupId=io.hops -DartifactId=hadoop-common -Dversion=3.2.0.3-SNAPSHOT -Dpackaging=jar -DgeneratePom=true

build: mvn-client-install
	mvn clean compile assembly:single

install-slave: build
	cp -f target/hop-experiments-1.0-SNAPSHOT-jar-with-dependencies.jar slave.target/
	cp -f slave.properties slave.target/
	cp -f target/hop-experiments-1.0-SNAPSHOT-jar-with-dependencies.jar ~/hammer-bench-slave/hop-experiments-1.0-SNAPSHOT-jar-with-dependencies.jar

bench:
	java -Dsun.io.serialization.extendedDebugInfo=true -Xmx24g -Xms24g -XX:+UseConcMarkSweepGC -XX:+UnlockDiagnosticVMOptions -XX:ParGCCardsPerStrideChunk=4096 -XX:+CMSScavengeBeforeRemark -XX:MaxGCPauseMillis=350 -XX:MaxTenuringThreshold=2 -XX:MaxNewSize=32000m -XX:+CMSClassUnloadingEnabled -XX:+ScavengeBeforeFullGC -verbose:gc -XX:+PrintGCTimeStamps -XX:+PrintGCDetails -cp target/hop-experiments-1.0-SNAPSHOT-jar-with-dependencies.jar io.hops.experiments.controller.Master

start-slave:
	cd slave.target && java -Dsun.io.serialization.extendedDebugInfo=true -Xmx48g -Xms48g -XX:+UseConcMarkSweepGC -XX:+UnlockDiagnosticVMOptions -XX:ParGCCardsPerStrideChunk=4096 -XX:+CMSScavengeBeforeRemark -XX:MaxGCPauseMillis=350 -XX:MaxTenuringThreshold=2 -XX:MaxNewSize=32000m -XX:+CMSClassUnloadingEnabled -XX:+ScavengeBeforeFullGC -verbose:gc -XX:+PrintGCTimeStamps -XX:+PrintGCDetails -cp "hop-experiments-1.0-SNAPSHOT-jar-with-dependencies.jar$(EXTRA_JAVAPATH)" io.hops.experiments.controller.Slave 1>/dev/null 2>&1 &