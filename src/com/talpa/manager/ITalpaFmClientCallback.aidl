package com.talpa.manager;

interface ITalpaFmClientCallback {
	void updateStation(String station);
	void updateAntenna(boolean b);
	void updateIsPlay(boolean isPlay);
	void updateTurnFinished(String station);
	void initPlayState(boolean isPlay,String station);
}

