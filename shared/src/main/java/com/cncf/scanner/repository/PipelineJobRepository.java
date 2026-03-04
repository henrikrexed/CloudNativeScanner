package com.cncf.scanner.repository;

import com.cncf.scanner.model.PipelineJob;
import com.cncf.scanner.model.PipelineJob.Stage;
import com.cncf.scanner.model.PipelineJob.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PipelineJobRepository extends JpaRepository<PipelineJob, Long> {

    /**
     * Claim the next pending job for a given stage using SKIP LOCKED.
     * This is safe for concurrent consumers — each caller gets a different job.
     */
    @Query(value = """
            UPDATE pipeline_jobs
            SET status = 'PROCESSING', claimed_at = NOW()
            WHERE id = (
                SELECT id FROM pipeline_jobs
                WHERE stage = :stage AND status = 'PENDING'
                ORDER BY created_at
                LIMIT 1
                FOR UPDATE SKIP LOCKED
            )
            RETURNING *
            """, nativeQuery = true)
    Optional<PipelineJob> claimNextJob(@Param("stage") String stage);

    /**
     * Reset stale jobs that have been processing for too long (stuck workers).
     */
    @Modifying
    @Query(value = """
            UPDATE pipeline_jobs
            SET status = 'PENDING', claimed_at = NULL
            WHERE status = 'PROCESSING'
            AND claimed_at < :threshold
            """, nativeQuery = true)
    int resetStaleJobs(@Param("threshold") LocalDateTime threshold);

    /**
     * Count jobs by stage and status (for monitoring dashboard).
     */
    @Query("SELECT j.status, COUNT(j) FROM PipelineJob j WHERE j.stage = :stage GROUP BY j.status")
    List<Object[]> countByStageGroupedByStatus(@Param("stage") Stage stage);

    /**
     * Count all pending jobs across all stages.
     */
    long countByStatus(Status status);

    /**
     * Count pending jobs for a specific stage.
     */
    long countByStageAndStatus(Stage stage, Status status);

    /**
     * Find failed jobs for monitoring (most recent first).
     */
    @Query("SELECT j FROM PipelineJob j WHERE j.status = 'FAILED' ORDER BY j.createdAt DESC")
    List<PipelineJob> findRecentFailures(org.springframework.data.domain.Pageable pageable);
}
