ALTER TABLE	hdfs_aces	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_block_checksum	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_block_infos	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_block_lookup_table	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_cache_directive	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_cache_directive_path	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_cache_pool	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_cached_block	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_corrupt_replicas	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_encoding_jobs	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_encoding_status	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_excess_replicas	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_groups	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_hash_buckets	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_inmemory_file_inode_data	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_inode_attributes	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_inode_dataset_lookup	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_inodes	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_invalidated_blocks	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_le_descriptors	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_lease_paths	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_leases	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_metadata_log	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_misreplicated_range_queue	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_on_going_sub_tree_ops	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_ondisk_large_file_inode_data	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_ondisk_medium_file_inode_data	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_ondisk_small_file_inode_data	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_path_memcached	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_pending_blocks	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_quota_update	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_repair_jobs	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_replica_under_constructions	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_replicas	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_retry_cache_entry	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_safe_blocks	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_storage_id_map	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_storages	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_under_replicated_blocks	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_users	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_users_groups	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	hdfs_variables	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_applicationattemptstate	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_applicationstate	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_container_to_decrease	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_container_to_signal	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_containerid_toclean	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_containers_checkpoint	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_containers_logs	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_containerstatus	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_delegation_key	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_delegation_token	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_le_descriptors	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_nextheartbeat	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_pendingevents	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_price_multiplicator	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_projects_daily_cost	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_projects_quota	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_reservation_state	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_resource	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_rmnode	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_rmnode_applications	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_rms_load	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';
ALTER TABLE	yarn_updatedcontainerinfo	ALGORITHM=INPLACE, COMMENT='NDB_TABLE=READ_BACKUP=1';