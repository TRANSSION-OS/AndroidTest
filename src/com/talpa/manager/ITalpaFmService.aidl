package com.talpa.manager;
import com.talpa.manager.ITalpaFmClientCallback; 
interface ITalpaFmService {
	void seekStation(int station, boolean direction);
	void tuneDecreaseStation(int station);
	void tuneIncreaseStation(int station);
	void play();
	
		//用来注册回调的对象  
    void registerCallback(ITalpaFmClientCallback cb);     
    void unregisterCallback(ITalpaFmClientCallback cb); 
}

