/**
 * jQuery.apTextCounter - Textbox/textarea counter tool.
 * 
 * http://www.blog.adampresley.com
 * 
 * This file is part of jQuery.apTextCounter
 *
 * jQuery.apTextCounter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jQuery.apTextCounter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jQuery.apTextCounter.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Callbacks:
 * 	- onCharacterCountChecked
 * 	- onMaxCharactersReached
 * 	- onTrackerUpdated
 * 
 * @author Adam Presley
 * @copyright Copyright (c) 2009 Adam Presley
 * @param {Object} config
 */

(function( $ ){

$.fn.apTextCounter = function(config)
{
	function __checkCount(el, e, __c)
	{
		var count = __c.maxCharacters - $(el).val().length;
		var countDisplay = (__c.direction !== "down") ? (__c.maxCharacters - count) : count;
		
		if (count <= 0)
		{
			var k = e.which;
			
			if ((k > 47 && k < 91) || (k > 95 && k < 112) || (k > 184 && k < 250) || k == 13 || k == 32)
			{
				e.preventDefault();
				e.stopPropagation();
				
				/*
				 * Fire any callbacks.
				 */
				if (__c.onMaxCharactersReached !== null)
				{
					if (__c.scope !== null)
					{
						__c.onMaxCharactersReached.call(__c.scope, {
							count: countDisplay,
							config: __c
						});
					}
					else
					{
						__c.onMaxCharactersReached({
							count: countDisplay,
							config: __c
						});
					}
				}
				
				return countDisplay;
			}
		}
		
		/*
		 * Fire any callbacks.
		 */
		if (__c.onCharacterCountChecked !== null)
		{
			if (__c.scope !== null)
			{
				__c.onCharacterCountChecked.call(__c.scope, {
					count: countDisplay,
					config: __c
				});
			}
			else
			{
				__c.onCharacterCountChecked({
					count: countDisplay,
					config: __c
				});
			}
		}

		return countDisplay;
	}
	
	function __updateTracker(el, count, __c)
	{
		var msg = __c.trackerTemplate.replace(/%s/i, count);
		$(__c.tracker).html(msg);
		
		/*
		 * Fire any callbacks.
		 */
		if (__c.onTrackerUpdated !== null)
		{
			if (__c.scope !== null)
			{
				__c.onTrackerUpdated.call(__c.scope, {
					count: count,
					config: __c
				});
			}
			else
			{
				__c.onTrackerUpdated({
					count: count,
					config: __c
				});
			}
		}
	}
	
	return this.each(function()
	{
		var __this = this;
		
		/*
		 * Store our configuration.
		 */
		this.__c = $.extend({
			maxCharacters: 141,
			direction: "down",
			tracker: "#tracker",
			trackerTemplate: "%s",
			
			scope: null,
			onCharacterCountChecked: null,
			onMaxCharactersReached: null,
			onTrackerUpdated: null
		}, config);

		/*
		 * Perform an initial check and setup the tracker.
		 */
		__checkCount(this, null, this.__c);
		__updateTracker(this, (this.__c.direction === "down") ? this.__c.maxCharacters : 0, this.__c);
		
		/*
		 * Assign the keydown and keyup handlers.
		 */
		$(this).keyup(function(e) {
			var count = __checkCount(__this, e, __this.__c);
			__updateTracker(__this, count, __this.__c);
		});
		
		$(this).keydown(function(e) {
			var count = __checkCount(__this, e, __this.__c);
			__updateTracker(__this, count, __this.__c);
		});
	});
};

})( jQuery );

