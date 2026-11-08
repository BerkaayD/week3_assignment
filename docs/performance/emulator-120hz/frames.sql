-- A SurfaceFrame ends when the app finishes the buffer. The corresponding
-- DisplayFrame ends at presentation. Count one presentation per display token.
WITH display_frames AS (
    SELECT display_frame_token, MAX(ts + dur) AS present_ns
    FROM actual_frame_timeline_slice
    JOIN process USING (upid)
    WHERE process.name = '/system/bin/surfaceflinger'
      AND surface_frame_token IS NULL AND dur >= 0
      AND present_type IN ('Early Present', 'On-time Present', 'Late Present')
    GROUP BY display_frame_token
)
SELECT a.ts, a.dur, a.display_frame_token, a.surface_frame_token,
       a.present_type, a.on_time_finish, a.jank_type, d.present_ns
FROM actual_frame_timeline_slice AS a
JOIN process AS p USING (upid)
LEFT JOIN display_frames AS d ON d.display_frame_token = a.display_frame_token
WHERE p.name = 'com.example.week3assignment'
  AND a.layer_name GLOB '*com.example.week3assignment/com.example.week3assignment.MainActivity#*'
  AND EXTRACT_ARG(a.arg_set_id, 'Is Buffer?') = 'Yes'
ORDER BY a.ts;
