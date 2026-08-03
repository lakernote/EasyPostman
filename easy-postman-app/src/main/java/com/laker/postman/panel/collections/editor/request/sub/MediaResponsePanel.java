package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.button.PauseButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Lightweight media response preview.
 * Audio uses Java Sound when the JDK supports the encoding; unsupported audio and video
 * can still be opened with the operating system's player.
 */
final class MediaResponsePanel extends JPanel {
    private static final int PROGRESS_MAX = 1_000;
    private static final long COMPLETION_TOLERANCE_MICROS = 20_000L;

    private final StartButton playButton = new StartButton();
    private final PauseButton pauseButton = new PauseButton();
    private final StopButton stopButton = new StopButton();
    private final JSlider progressSlider = new JSlider(0, PROGRESS_MAX, 0);
    private final JLabel titleLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel fileSummaryLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel timeLabel = new JLabel(formatTime(0) + " / " + formatTime(0));
    private final JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton openSystemPlayerButton = ModernButtonFactory.createCompactButton(
            I18nUtil.getMessage(MessageKeys.RESPONSE_MEDIA_OPEN_SYSTEM), false, null);
    private final Timer progressTimer = new Timer(200, event -> updateProgress());
    private final JPanel audioControlsPanel;

    private File sourceFile;
    private Clip clip;
    private SwingWorker<Clip, Void> loadWorker;
    private long sourceGeneration;
    private boolean loading;
    private boolean paused;
    private boolean builtInPlaybackUnavailable;
    private boolean updatingProgress;
    private MediaKind mediaKind = MediaKind.AUDIO;

    MediaResponsePanel() {
        setLayout(new GridBagLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));

        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        titleLabel.setForeground(ModernColors.getTextPrimary());

        fileSummaryLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        fileSummaryLabel.setForeground(ModernColors.getTextSecondary());
        keepLineVisibleWhenShrunk(fileSummaryLabel);
        timeLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        timeLabel.setForeground(ModernColors.getTextSecondary());
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        keepLineVisibleWhenShrunk(statusLabel);

        audioControlsPanel = createControlsPanel();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 6, 0);
        add(titleLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 14, 0);
        add(fileSummaryLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 10, 0);
        add(audioControlsPanel, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 10, 0);
        add(statusLabel, gbc);

        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(openSystemPlayerButton, gbc);

        playButton.addActionListener(event -> play());
        pauseButton.addActionListener(event -> pause());
        stopButton.addActionListener(event -> stop());
        openSystemPlayerButton.addActionListener(event -> openWithSystemPlayer());
        progressSlider.addChangeListener(event -> seekFromSlider());

        updateMediaPresentation();
        refreshControlState();
    }

    private JPanel createControlsPanel() {
        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 2);
        controls.add(playButton, gbc);

        gbc.gridx = 1;
        controls.add(pauseButton, gbc);

        gbc.gridx = 2;
        gbc.insets = new Insets(0, 0, 0, 8);
        controls.add(stopButton, gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 8);
        controls.add(progressSlider, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        controls.add(timeLabel, gbc);
        return controls;
    }

    void setAudioSource(String filePath, String fileName, long bodySize) {
        setMediaSource(MediaKind.AUDIO, filePath, fileName, bodySize);
    }

    void setVideoSource(String filePath, String fileName, long bodySize) {
        setMediaSource(MediaKind.VIDEO, filePath, fileName, bodySize);
    }

    private void setMediaSource(MediaKind kind, String filePath, String fileName, long bodySize) {
        clear();
        mediaKind = kind;
        updateMediaPresentation();
        sourceFile = filePath == null ? null : new File(filePath);

        String resolvedName = fileName;
        if (resolvedName == null || resolvedName.isBlank()) {
            resolvedName = sourceFile == null ? "" : sourceFile.getName();
        }
        fileSummaryLabel.setText(I18nUtil.getMessage(
                MessageKeys.RESPONSE_MEDIA_FILE_SUMMARY,
                abbreviate(resolvedName, 72),
                ResponseSizeCalculator.formatBytes(Math.max(0, bodySize))
        ));
        fileSummaryLabel.setToolTipText(resolvedName);

        if (sourceFile == null || !sourceFile.isFile() || !sourceFile.canRead()) {
            setStatus(MessageKeys.RESPONSE_MEDIA_FILE_UNAVAILABLE, true);
        } else if (mediaKind == MediaKind.AUDIO) {
            setStatus(MessageKeys.RESPONSE_AUDIO_READY, false);
        }
        refreshControlState();
    }

    void clear() {
        sourceGeneration++;
        // Let an in-flight decoder finish so its Clip can be closed in done(); cancelling a
        // SwingWorker after Clip allocation can discard the result and leak the native line.
        loadWorker = null;
        loading = false;
        paused = false;
        builtInPlaybackUnavailable = false;
        progressTimer.stop();
        closeClip();
        sourceFile = null;
        mediaKind = MediaKind.AUDIO;
        fileSummaryLabel.setText("");
        fileSummaryLabel.setToolTipText(null);
        updateProgressDisplay(0, 0);
        updateMediaPresentation();
        refreshControlState();
    }

    private void updateMediaPresentation() {
        boolean audio = mediaKind == MediaKind.AUDIO;
        titleLabel.setText(I18nUtil.getMessage(audio
                ? MessageKeys.RESPONSE_AUDIO_TITLE
                : MessageKeys.RESPONSE_VIDEO_TITLE));
        audioControlsPanel.setVisible(audio);
        if (audio) {
            setStatus(MessageKeys.RESPONSE_AUDIO_READY, false);
        } else {
            hideStatus();
        }
    }

    private void play() {
        if (clip != null) {
            startClip();
            return;
        }
        if (mediaKind != MediaKind.AUDIO || loading || builtInPlaybackUnavailable || !hasReadableSource()) {
            return;
        }

        File requestedFile = sourceFile;
        long requestedGeneration = sourceGeneration;
        loading = true;
        setStatus(MessageKeys.RESPONSE_AUDIO_LOADING, false);
        refreshControlState();

        loadWorker = new SwingWorker<>() {
            @Override
            protected Clip doInBackground() throws Exception {
                return loadClip(requestedFile);
            }

            @Override
            protected void done() {
                try {
                    Clip loadedClip = get();
                    if (requestedGeneration != sourceGeneration) {
                        loadedClip.close();
                        return;
                    }
                    loading = false;
                    clip = loadedClip;
                    clip.addLineListener(MediaResponsePanel.this::handleLineEvent);
                    updateProgressDisplay(0, clip.getMicrosecondLength());
                    startClip();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (requestedGeneration == sourceGeneration) {
                        loading = false;
                        markBuiltInPlaybackUnavailable(e);
                    }
                } catch (ExecutionException e) {
                    if (requestedGeneration == sourceGeneration) {
                        loading = false;
                        markBuiltInPlaybackUnavailable(e.getCause() != null ? e.getCause() : e);
                    }
                } finally {
                    if (requestedGeneration == sourceGeneration) {
                        loadWorker = null;
                    }
                    refreshControlState();
                }
            }
        };
        loadWorker.execute();
    }

    private Clip loadClip(File file) throws Exception {
        Clip loadedClip = AudioSystem.getClip();
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file)) {
            loadedClip.open(audioInputStream);
            return loadedClip;
        } catch (Exception e) {
            loadedClip.close();
            throw e;
        }
    }

    private void startClip() {
        if (clip == null) {
            return;
        }
        long duration = clip.getMicrosecondLength();
        if (duration > 0 && clip.getMicrosecondPosition() >= duration - COMPLETION_TOLERANCE_MICROS) {
            clip.setMicrosecondPosition(0);
        }
        paused = false;
        clip.start();
        progressTimer.start();
        setStatus(MessageKeys.RESPONSE_AUDIO_PLAYING, false);
        refreshControlState();
    }

    private void pause() {
        if (clip == null || !clip.isRunning()) {
            return;
        }
        paused = true;
        clip.stop();
        progressTimer.stop();
        updateProgress();
        setStatus(MessageKeys.RESPONSE_AUDIO_PAUSED, false);
        refreshControlState();
    }

    private void stop() {
        if (clip == null) {
            return;
        }
        paused = false;
        clip.stop();
        clip.setMicrosecondPosition(0);
        progressTimer.stop();
        updateProgressDisplay(0, clip.getMicrosecondLength());
        setStatus(MessageKeys.RESPONSE_AUDIO_READY, false);
        refreshControlState();
    }

    private void seekFromSlider() {
        if (updatingProgress || clip == null) {
            return;
        }
        long duration = clip.getMicrosecondLength();
        long target = duration * progressSlider.getValue() / PROGRESS_MAX;
        if (progressSlider.getValueIsAdjusting()) {
            timeLabel.setText(formatTime(target) + " / " + formatTime(duration));
            return;
        }
        clip.setMicrosecondPosition(target);
        updateProgressDisplay(target, duration);
    }

    private void updateProgress() {
        if (clip == null) {
            return;
        }
        long position = clip.getMicrosecondPosition();
        long duration = clip.getMicrosecondLength();
        if (!progressSlider.getValueIsAdjusting()) {
            updateProgressDisplay(position, duration);
        }
        if (!clip.isRunning() && !paused
                && duration > 0 && position >= duration - COMPLETION_TOLERANCE_MICROS) {
            clip.setMicrosecondPosition(0);
            progressTimer.stop();
            updateProgressDisplay(0, duration);
            setStatus(MessageKeys.RESPONSE_AUDIO_READY, false);
            refreshControlState();
        }
    }

    private void updateProgressDisplay(long position, long duration) {
        updatingProgress = true;
        try {
            int value = duration <= 0 ? 0 : (int) Math.min(PROGRESS_MAX, position * PROGRESS_MAX / duration);
            progressSlider.setValue(value);
            timeLabel.setText(formatTime(position) + " / " + formatTime(duration));
        } finally {
            updatingProgress = false;
        }
    }

    private void handleLineEvent(LineEvent event) {
        if (event.getType() == LineEvent.Type.STOP) {
            SwingUtilities.invokeLater(this::updateProgress);
        }
    }

    private void markBuiltInPlaybackUnavailable(Throwable error) {
        builtInPlaybackUnavailable = true;
        progressTimer.stop();
        closeClip();
        if (error instanceof UnsupportedAudioFileException || error instanceof IllegalArgumentException) {
            setStatus(MessageKeys.RESPONSE_AUDIO_UNSUPPORTED, true);
        } else {
            String message = error == null || error.getMessage() == null
                    ? error == null ? "" : error.getClass().getSimpleName()
                    : error.getMessage();
            setStatus(MessageKeys.RESPONSE_AUDIO_LOAD_FAILED, true, message);
        }
    }

    private void openWithSystemPlayer() {
        if (!hasReadableSource()) {
            setStatus(MessageKeys.RESPONSE_MEDIA_FILE_UNAVAILABLE, true);
            return;
        }
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                setStatus(MessageKeys.RESPONSE_MEDIA_SYSTEM_UNAVAILABLE, true);
                return;
            }
            Desktop.getDesktop().open(sourceFile);
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            setStatus(MessageKeys.RESPONSE_MEDIA_OPEN_FAILED, true, message);
        }
    }

    private void refreshControlState() {
        boolean readable = hasReadableSource();
        boolean running = clip != null && clip.isRunning();
        boolean audio = mediaKind == MediaKind.AUDIO;
        playButton.setEnabled(isEnabled() && audio && readable && !loading && !running
                && !builtInPlaybackUnavailable);
        pauseButton.setEnabled(isEnabled() && audio && running);
        stopButton.setEnabled(isEnabled() && audio && clip != null
                && (running || paused || clip.getMicrosecondPosition() > 0));
        progressSlider.setEnabled(isEnabled() && audio && clip != null);
        openSystemPlayerButton.setEnabled(isEnabled() && readable);
    }

    private boolean hasReadableSource() {
        return sourceFile != null && sourceFile.isFile() && sourceFile.canRead();
    }

    private void setStatus(String messageKey, boolean error, Object... arguments) {
        String text = I18nUtil.getMessage(messageKey, arguments);
        statusLabel.setText(text);
        statusLabel.setToolTipText(text);
        statusLabel.setForeground(error ? ModernColors.getError() : ModernColors.getTextSecondary());
        statusLabel.setVisible(true);
    }

    private void hideStatus() {
        statusLabel.setText("");
        statusLabel.setToolTipText(null);
        statusLabel.setVisible(false);
    }

    private void closeClip() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        refreshControlState();
    }

    @Override
    public void removeNotify() {
        releasePlaybackResources();
        super.removeNotify();
    }

    private void releasePlaybackResources() {
        sourceGeneration++;
        // The generation guard closes any Clip produced by an older background load.
        loadWorker = null;
        loading = false;
        paused = false;
        builtInPlaybackUnavailable = false;
        progressTimer.stop();
        closeClip();
        updateProgressDisplay(0, 0);
        if (hasReadableSource()) {
            if (mediaKind == MediaKind.AUDIO) {
                setStatus(MessageKeys.RESPONSE_AUDIO_READY, false);
            } else {
                hideStatus();
            }
        } else {
            setStatus(MessageKeys.RESPONSE_MEDIA_FILE_UNAVAILABLE, true);
        }
        refreshControlState();
    }

    private static void keepLineVisibleWhenShrunk(JLabel label) {
        // These labels are empty during construction. Freezing their current preferred height
        // would set a zero-height minimum, so GridBagLayout could collapse the row on narrow
        // response panes. Preserve one text line while still allowing horizontal shrinkage.
        int lineHeight = label.getFontMetrics(label.getFont()).getHeight();
        label.setMinimumSize(new Dimension(0, lineHeight));
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        int suffixLength = Math.max(8, maxLength / 3);
        return value.substring(0, maxLength - suffixLength - 1)
                + "…"
                + value.substring(value.length() - suffixLength);
    }

    private static String formatTime(long microseconds) {
        long totalSeconds = Math.max(0, microseconds) / 1_000_000L;
        long hours = totalSeconds / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private enum MediaKind {
        AUDIO,
        VIDEO
    }
}
