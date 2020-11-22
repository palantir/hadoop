# 2.9.2-palantir.14

Based on Apache Hadoop tag `release-2.9.2-RC0`.

* Backport [HADOOP-16960](https://issues.apache.org/jira/browse/HADOOP-16960) - needs [HADOOP-15185](https://issues.apache.org/jira/browse/HADOOP-15185), [HADOOP-15186](https://issues.apache.org/jira/browse/HADOOP-15186) and [HADOOP-15342](https://issues.apache.org/jira/browse/HADOOP-15342) applied beforehand
* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Backport [HADOOP-14652](https://issues.apache.org/jira/browse/HADOOP-14652)
* Upgrade the AWS Java SDK from 1.11.199 to 1.11.201
* Backport [HDFS-12670](https://jira.apache.org/jira/browse/HDFS-12670)
* Manual backport of [HADOOP-15265](https://issues.apache.org/jira/browse/HADOOP-15265)
* Backport log-level changes to S3AFileSystem#deleteObjects from [HADOOP-15176](https://issues.apache.org/jira/browse/HADOOP-15176)
* Initial implementation of [HADOOP-16132](https://issues.apache.org/jira/browse/HADOOP-16132)
* Backport [HADOOP-16248](https://issues.apache.org/jira/browse/HADOOP-16248)
* Backport [HADOOP-12760](https://issues.apache.org/jira/browse/HADOOP-12760)
* Backport [HADOOP-11858](https://issues.apache.org/jira/browse/HADOOP-11858)
* Manual backport of [HADOOP-15938](https://issues.apache.org/jira/browse/HADOOP-15938)
* Comment out s3guard quantile creation in S3AInstrumentation as more aggressive version of [HADOOP-16278](https://issues.apache.org/jira/browse/HADOOP-16278)
* Use wildfly-openssl in Azure Blob Store connector
* Catch NPEs in RMContainerAllocator#handleJobPriorityChange for compatibility with older clusters
* Initial patch for [MAPREDUCE-7292](https://issues.apache.org/jira/browse/MAPREDUCE-7292)
* Upgrade commons-configuration to 1.7 and jsp-api to 2.1.1
* Backport [HADOOP-16588](https://issues.apache.org/jira/browse/HADOOP-16588)

# 2.9.2-palantir.13

Based on Apache Hadoop tag `release-2.9.2-RC0`.

* Backport [HADOOP-16960](https://issues.apache.org/jira/browse/HADOOP-16960) - needs [HADOOP-15185](https://issues.apache.org/jira/browse/HADOOP-15185), [HADOOP-15186](https://issues.apache.org/jira/browse/HADOOP-15186) and [HADOOP-15342](https://issues.apache.org/jira/browse/HADOOP-15342) applied beforehand
* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Backport [HADOOP-14652](https://issues.apache.org/jira/browse/HADOOP-14652)
* Upgrade the AWS Java SDK from 1.11.199 to 1.11.201
* Backport [HDFS-12670](https://jira.apache.org/jira/browse/HDFS-12670)
* Manual backport of [HADOOP-15265](https://issues.apache.org/jira/browse/HADOOP-15265)
* Backport log-level changes to S3AFileSystem#deleteObjects from [HADOOP-15176](https://issues.apache.org/jira/browse/HADOOP-15176)
* Initial implementation of [HADOOP-16132](https://issues.apache.org/jira/browse/HADOOP-16132)
* Backport [HADOOP-16248](https://issues.apache.org/jira/browse/HADOOP-16248)
* Backport [HADOOP-12760](https://issues.apache.org/jira/browse/HADOOP-12760)
* Backport [HADOOP-11858](https://issues.apache.org/jira/browse/HADOOP-11858)
* Manual backport of [HADOOP-15938](https://issues.apache.org/jira/browse/HADOOP-15938)
* Comment out s3guard quantile creation in S3AInstrumentation as more aggressive version of [HADOOP-16278](https://issues.apache.org/jira/browse/HADOOP-16278)
* Use wildfly-openssl in Azure Blob Store connector
* Catch NPEs in RMContainerAllocator#handleJobPriorityChange for compatibility with older clusters
* Initial patch for [MAPREDUCE-7292](https://issues.apache.org/jira/browse/MAPREDUCE-7292)

# 2.9.2-palantir.12

Based on Apache Hadoop tag `release-2.9.2-RC0`.

* Backport [HADOOP-16960](https://issues.apache.org/jira/browse/HADOOP-16960) - needs [HADOOP-15185](https://issues.apache.org/jira/browse/HADOOP-15185), [HADOOP-15186](https://issues.apache.org/jira/browse/HADOOP-15186) and [HADOOP-15342](https://issues.apache.org/jira/browse/HADOOP-15342) applied beforehand
* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Backport [HADOOP-14652](https://issues.apache.org/jira/browse/HADOOP-14652)
* Upgrade the AWS Java SDK from 1.11.199 to 1.11.201
* Backport [HDFS-12670](https://jira.apache.org/jira/browse/HDFS-12670)
* Manual backport of [HADOOP-15265](https://issues.apache.org/jira/browse/HADOOP-15265)
* Backport log-level changes to S3AFileSystem#deleteObjects from [HADOOP-15176](https://issues.apache.org/jira/browse/HADOOP-15176)
* Initial implementation of [HADOOP-16132](https://issues.apache.org/jira/browse/HADOOP-16132)
* Backport [HADOOP-16248](https://issues.apache.org/jira/browse/HADOOP-16248)
* Backport [HADOOP-12760](https://issues.apache.org/jira/browse/HADOOP-12760)
* Backport [HADOOP-11858](https://issues.apache.org/jira/browse/HADOOP-11858)
* Manual backport of [HADOOP-15938](https://issues.apache.org/jira/browse/HADOOP-15938)
* Comment out s3guard quantile creation in S3AInstrumentation as more aggressive version of [HADOOP-16278](https://issues.apache.org/jira/browse/HADOOP-16278)
* Use wildfly-openssl in Azure Blob Store connector
* Catch NPEs in RMContainerAllocator#handleJobPriorityChange for compatibility with older clusters

# 2.9.2-palantir.11

Based on Apache Hadoop tag `release-2.9.2-RC0`.

* Backport [HADOOP-16960](https://issues.apache.org/jira/browse/HADOOP-16960) - needs [HADOOP-15185](https://issues.apache.org/jira/browse/HADOOP-15185), [HADOOP-15186](https://issues.apache.org/jira/browse/HADOOP-15186) and [HADOOP-15342](https://issues.apache.org/jira/browse/HADOOP-15342) applied beforehand
* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Backport [HADOOP-14652](https://issues.apache.org/jira/browse/HADOOP-14652)
* Upgrade the AWS Java SDK from 1.11.199 to 1.11.201
* Backport [HDFS-12670](https://jira.apache.org/jira/browse/HDFS-12670)
* Manual backport of [HADOOP-15265](https://issues.apache.org/jira/browse/HADOOP-15265)
* Backport log-level changes to S3AFileSystem#deleteObjects from [HADOOP-15176](https://issues.apache.org/jira/browse/HADOOP-15176)
* Initial implementation of [HADOOP-16132](https://issues.apache.org/jira/browse/HADOOP-16132)
* Backport [HADOOP-16248](https://issues.apache.org/jira/browse/HADOOP-16248)
* Backport [HADOOP-12760](https://issues.apache.org/jira/browse/HADOOP-12760)
* Backport [HADOOP-11858](https://issues.apache.org/jira/browse/HADOOP-11858)
* Manual backport of [HADOOP-15938](https://issues.apache.org/jira/browse/HADOOP-15938)
* Comment out s3guard quantile creation in S3AInstrumentation as more aggressive version of [HADOOP-16278](https://issues.apache.org/jira/browse/HADOOP-16278)
* Use wildfly-openssl in Azure Blob Store connector

# 2.9.2-palantir.10

Based on Apache Hadoop tag `release-2.9.2-RC0`.

* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Backport [HADOOP-14652](https://issues.apache.org/jira/browse/HADOOP-14652)
* Upgrade the AWS Java SDK from 1.11.199 to 1.11.201
* Backport [HDFS-12670](https://jira.apache.org/jira/browse/HDFS-12670)
* Manual backport of [HADOOP-15265](https://issues.apache.org/jira/browse/HADOOP-15265)
* Backport log-level changes to S3AFileSystem#deleteObjects from [HADOOP-15176](https://issues.apache.org/jira/browse/HADOOP-15176)
* Initial implementation of [HADOOP-16132](https://issues.apache.org/jira/browse/HADOOP-16132)
* Backport [HADOOP-16248](https://issues.apache.org/jira/browse/HADOOP-16248)
* Backport [HADOOP-12760](https://issues.apache.org/jira/browse/HADOOP-12760)
* Backport [HADOOP-11858](https://issues.apache.org/jira/browse/HADOOP-11858)
* Manual backport of [HADOOP-15938](https://issues.apache.org/jira/browse/HADOOP-15938)
* Comment out s3guard quantile creation in S3AInstrumentation as more aggressive version of [HADOOP-16278](https://issues.apache.org/jira/browse/HADOOP-16278)
* Use wildfly-openssl in Azure Blob Store connector

# 2.9.2-palantir.9

Based on Apache Hadoop tag `release-2.9.2-RC0`.

* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Backport [HADOOP-14652](https://issues.apache.org/jira/browse/HADOOP-14652)
* Upgrade the AWS Java SDK from 1.11.199 to 1.11.201
* Backport [HDFS-12670](https://jira.apache.org/jira/browse/HDFS-12670)
* Manual backport of [HADOOP-15265](https://issues.apache.org/jira/browse/HADOOP-15265)
* Backport log-level changes to S3AFileSystem#deleteObjects from [HADOOP-15176](https://issues.apache.org/jira/browse/HADOOP-15176)
* Initial implementation of [HADOOP-16132](https://issues.apache.org/jira/browse/HADOOP-16132)
* Backport [HADOOP-16248](https://issues.apache.org/jira/browse/HADOOP-16248)
* Backport [HADOOP-12760](https://issues.apache.org/jira/browse/HADOOP-12760)
* Backport [HADOOP-11858](https://issues.apache.org/jira/browse/HADOOP-11858)
* Manual backport of [HADOOP-15938](https://issues.apache.org/jira/browse/HADOOP-15938)
* Comment out s3guard quantile creation in S3AInstrumentation as more aggressive version of [HADOOP-16278](https://issues.apache.org/jira/browse/HADOOP-16278)

# 2.9.2-palantir.8

Based on Apache Hadoop tag `release-2.9.2-RC0`.

* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Backport [HADOOP-14652](https://issues.apache.org/jira/browse/HADOOP-14652)
* Upgrade the AWS Java SDK from 1.11.199 to 1.11.201
* Backport [HDFS-12670](https://jira.apache.org/jira/browse/HDFS-12670)
* Manual backport of [HADOOP-15265](https://issues.apache.org/jira/browse/HADOOP-15265)
* Backport log-level changes to S3AFileSystem#deleteObjects from [HADOOP-15176](https://issues.apache.org/jira/browse/HADOOP-15176)
* Initial implementation of [HADOOP-16132](https://issues.apache.org/jira/browse/HADOOP-16132)
* Backport [HADOOP-16248](https://issues.apache.org/jira/browse/HADOOP-16248)
* Backport [HADOOP-12760](https://issues.apache.org/jira/browse/HADOOP-12760)
* Backport [HADOOP-11858](https://issues.apache.org/jira/browse/HADOOP-11858)
* Manual backport of [HADOOP-15938](https://issues.apache.org/jira/browse/HADOOP-15938)

# 2.9.2-palantir.7

Based on Apache Hadoop tag `release-2.9.2-RC0`.

* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Backport [HADOOP-14652](https://issues.apache.org/jira/browse/HADOOP-14652)
* Upgrade the AWS Java SDK from 1.11.199 to 1.11.201
* Backport [HDFS-12670](https://jira.apache.org/jira/browse/HDFS-12670)
* Manual backport of [HADOOP-15265](https://issues.apache.org/jira/browse/HADOOP-15265)
* Backport log-level changes to S3AFileSystem#deleteObjects from [HADOOP-15176](https://issues.apache.org/jira/browse/HADOOP-15176)
* Initial implementation of [HADOOP-16132](https://issues.apache.org/jira/browse/HADOOP-16132)
* Backport [HADOOP-16248](https://issues.apache.org/jira/browse/HADOOP-16248)
* Backport [HADOOP-12760](https://issues.apache.org/jira/browse/HADOOP-12760)

# 2.9.2-palantir.6

Based on Apache Hadoop tag `release-2.9.2-RC0`.

* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Backport [HADOOP-14652](https://issues.apache.org/jira/browse/HADOOP-14652)
* Upgrade the AWS Java SDK from 1.11.199 to 1.11.201
* Backport [HDFS-12670](https://jira.apache.org/jira/browse/HDFS-12670)
* Manual backport of [HADOOP-15265](https://issues.apache.org/jira/browse/HADOOP-15265)
* Backport log-level changes to S3AFileSystem#deleteObjects from [HADOOP-15176](https://issues.apache.org/jira/browse/HADOOP-15176)
* Initial implementation of [HADOOP-16132](https://issues.apache.org/jira/browse/HADOOP-16132)
* Initial implementation of [HADOOP-16248](https://issues.apache.org/jira/browse/HADOOP-16248)

# 2.9.2-palantir.5

Based on Apache Hadoop tag `release-2.9.2-RC0`.

* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Backport [HADOOP-14652](https://issues.apache.org/jira/browse/HADOOP-14652)
* Upgrade the AWS Java SDK from 1.11.199 to 1.11.201
* Backport [HDFS-12670](https://jira.apache.org/jira/browse/HDFS-12670)
* Manual backport of [HADOOP-15265](https://issues.apache.org/jira/browse/HADOOP-15265)
* Backport log-level changes to S3AFileSystem#deleteObjects from [HADOOP-15176](https://issues.apache.org/jira/browse/HADOOP-15176)
* Initial implementation of [HADOOP-16132](https://issues.apache.org/jira/browse/HADOOP-16132)
* Initial implementation of [HADOOP-16248](https://issues.apache.org/jira/browse/HADOOP-16248)

# 2.9.2-palantir.4

Based on Apache Hadoop tag `release-2.9.2-RC0`.

* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Backport [HADOOP-14652](https://issues.apache.org/jira/browse/HADOOP-14652)
* Upgrade the AWS Java SDK from 1.11.199 to 1.11.201
* Backport [HDFS-12670](https://jira.apache.org/jira/browse/HDFS-12670)
* Manual backport of [HADOOP-15265](https://issues.apache.org/jira/browse/HADOOP-15265)
* Backport log-level changes to S3AFileSystem#deleteObjects from [HADOOP-15176](https://issues.apache.org/jira/browse/HADOOP-15176)
* Initial implementation of [HADOOP-16132](https://issues.apache.org/jira/browse/HADOOP-16132)

# 2.9.2-palantir.3

Based on Apache Hadoop tag `release-2.9.2-RC0`.

* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Backport [HADOOP-14652](https://issues.apache.org/jira/browse/HADOOP-14652)
* Upgrade the AWS Java SDK from 1.11.199 to 1.11.201
* Backport [HDFS-12670](https://jira.apache.org/jira/browse/HDFS-12670)
* Manual backport of [HADOOP-15265](https://issues.apache.org/jira/browse/HADOOP-15265)
* Backport log-level changes to S3AFileSystem#deleteObjects from [HADOOP-15176](https://issues.apache.org/jira/browse/HADOOP-15176)
