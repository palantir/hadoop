# 2.8.0-palantir1

Based on branch-2.8.0, commit e53c4a6d69fdfcdc41afa38e51f1d9a06fb10f2a.

* Backport [HDFS-9276](https://issues.apache.org/jira/browse/HDFS-9276)
* Backport [HADOOP-12705](https://issues.apache.org/jira/browse/HADOOP-12705)
* Backport [HADOOP-13050](https://issues.apache.org/jira/browse/HADOOP-13050)
* Backport [HADOOP-13075](https://issues.apache.org/jira/browse/HADOOP-13075)
* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Apply in-progress patch for [HADOOP-14062](https://issues.apache.org/jira/browse/HADOOP-14062)

# 2.8.0-palantir2

Based on branch-2.8.0, commit 159b8b6ab8ba08604d266f774d0513a1fe9b2de0.

* Backport [HDFS-9276](https://issues.apache.org/jira/browse/HDFS-9276)
* Backport [HADOOP-12705](https://issues.apache.org/jira/browse/HADOOP-12705)
* Backport [HADOOP-13050](https://issues.apache.org/jira/browse/HADOOP-13050)
* Backport [HADOOP-13075](https://issues.apache.org/jira/browse/HADOOP-13075)
* Backport [HADOOP-14028](https://issues.apache.org/jira/browse/HADOOP-14028) from branch-2.
  It is also fixed on branch-2.8.0 upstream, but in a way that conflicts with HADOOP-13075.
  The branch-2 patch does not conflict.
* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
* Apply in-progress patch for [HADOOP-14062](https://issues.apache.org/jira/browse/HADOOP-14062)

# 2.8.0-palantir3

Based on branch-2.8.0, tag release-2.8.0-RC3 (commit 91f2b7a13d1e97be65db92ddabc627cc29ac0009).
In other words, this is based off of the actual Apache Hadoop 2.8.0 release.

* Backport [HDFS-9276](https://issues.apache.org/jira/browse/HDFS-9276)
* Backport [HADOOP-12705](https://issues.apache.org/jira/browse/HADOOP-12705)
* Backport [HADOOP-13050](https://issues.apache.org/jira/browse/HADOOP-13050)
* Backport [HADOOP-13075](https://issues.apache.org/jira/browse/HADOOP-13075)
* Backport [HADOOP-14028](https://issues.apache.org/jira/browse/HADOOP-14028) from branch-2.
  It is also fixed on branch-2.8.0 upstream, but in a way that conflicts with HADOOP-13075.
  The branch-2 patch does not conflict.
* Revert [HADOOP-13188](https://issues.apache.org/jira/browse/HADOOP-13188)
