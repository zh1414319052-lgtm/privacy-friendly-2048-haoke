package org.secuso.privacyfriendly2048.challenges;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static org.secuso.privacyfriendly2048.challenges.ChallengeGame.*;

public final class ChallengeStore {
    private final SharedPreferences preferences;

    public ChallengeStore(Context context) {
        preferences = context.getSharedPreferences("challenge_modes", Context.MODE_PRIVATE);
    }

    public ChallengeGame load(Setup setup) {
        ChallengeGame saved = read(setup);
        return saved == null ? new ChallengeGame(setup) : saved;
    }

    public boolean canContinue(Setup setup) {
        ChallengeGame saved = read(setup);
        return saved != null && saved.state().moves != 0 && saved.result() == Result.PLAYING;
    }

    private ChallengeGame read(Setup setup) {
        try {
            String text = preferences.getString("board_" + setup.key, null);
            if (text == null) return null;
            JSONObject data = new JSONObject(text);
            if (data.getInt("version") != 1) return null;
            State current = decode(data.getJSONObject("current"), setup);
            State previous = data.has("previous") ? decode(data.getJSONObject("previous"), setup) : null;
            if (previous != null && previous.moves != current.moves - 1) previous = null;
            return new ChallengeGame(setup, current, previous);
        } catch (JSONException | ClassCastException e) {
            Log.w("2048", "Could not read a challenge save", e);
            return null;
        }
    }

    public long record(Setup setup) {
        try {
            return Math.max(0L, preferences.getLong("record_" + setup.key, 0L));
        } catch (ClassCastException e) {
            return 0L;
        }
    }

    public boolean save(Setup setup, ChallengeGame game) {
        try {
            JSONObject data = new JSONObject().put("version", 1).put("current", encode(game.state()));
            if (game.canUndo()) data.put("previous", encode(game.previous()));
            SharedPreferences.Editor editor = preferences.edit().putString("board_" + setup.key, data.toString());
            long record = record(setup);
            if (setup.isPuzzle()) {
                int moves = game.state().moves;
                if (game.result() == Result.SOLVED && moves > 0 && (record == 0 || moves < record)) {
                    editor.putLong("record_" + setup.key, moves);
                }
            } else if (game.state().score > record) {
                editor.putLong("record_" + setup.key, game.state().score);
            }
            return editor.commit();
        } catch (JSONException e) {
            Log.e("2048", "Could not write a challenge save", e);
            return false;
        }
    }

    private static JSONObject encode(State snapshot) throws JSONException {
        JSONArray cells = new JSONArray();
        for (int i = 0; i < snapshot.cellCount(); i++) cells.put(snapshot.tile(i));
        return new JSONObject().put("cells", cells).put("score", snapshot.score)
                .put("moves", snapshot.moves).put("random", snapshot.random);
    }

    private static State decode(JSONObject data, Setup setup) throws JSONException {
        JSONArray cells = data.getJSONArray("cells");
        if (cells.length() != setup.size * setup.size) throw new JSONException("Invalid board size");
        int[] board = new int[cells.length()];
        for (int i = 0; i < board.length; i++) {
            board[i] = cells.getInt(i);
            if (!setup.validTile(i, board[i])) throw new JSONException("Invalid tile");
        }
        long score = data.getLong("score");
        int moves = data.getInt("moves");
        if (score < 0 || moves < 0 || (setup.isPuzzle() && moves > setup.limit)) {
            throw new JSONException("Invalid progress");
        }
        return new State(board, score, moves, data.getLong("random"));
    }
}
