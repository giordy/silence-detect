/*
 * Copyright (C) 2014 Giordano Battilana, Novadart
 *
 * This file is part of Silence-Detect.
 *
 * Silence-Detect is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Silence-Detect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Silence-Detect.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.novadart.silencedetect;

public class SilenceStats {
	
	private String totalDuration;
	private Double silenceDuration;
	
	public String getTotalDuration() {
		return totalDuration;
	}
	public void setTotalDuration(String totalDuration) {
		this.totalDuration = totalDuration;
	}
	public Double getSilenceDuration() {
		return silenceDuration;
	}
	public void setSilenceDuration(Double silenceDuration) {
		this.silenceDuration = silenceDuration;
	}

	public String getSilenceDurationHumanReadable() {
		if(silenceDuration < 60){
			return Math.ceil(silenceDuration) + " seconds";
		}
		
		else {
			int minutes = (int) Math.floor(silenceDuration / 60.0);
			int seconds = (int) Math.ceil(silenceDuration - (minutes*60) );
			
			return minutes + " minutes and "+seconds+" seconds";
		}
		
	}
	
}
