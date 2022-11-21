# Lovense LAN Library #

This library provides a simple java interface to control lovense toys using lovense connect.

Example using getToys()

```java
public class Test
{
	public static void main(String[] args)
	{
		//Trigger an immediate toy search and block until complete
		LovenseConnect.refresh();

		//Fetch a collection of all toys found on the local network
		Collection<LovenseToy> toys = LovenseConnect.getToys();

		// Cycle through each toy displaying its stats and setting the vibration to 50%
		Iterator<LovenseToy> toyIterator = toys.iterator();
		while(toyIterator.hasNext())
		{
		    LovenseToy nextToy = toyIterator.next();
		    System.err.println("  Instance: " + nextToy.toString());
		    System.err.println("  Nickname: " + nextToy.getNickname());
		    System.err.println("  Name: " + nextToy.getName());
		    System.err.println("  id: " + nextToy.getId());
		    System.err.println("  Battery:" + String.valueOf(nextToy.getBattery()));
		    nextToy.vibrate(10); // Set toy to 50% vibration (0-20)
		}
	}
}
```

Every call to getToys() also calls LovenseConnect.refreshIfNeeded() this will launch a thread to look for new devices using lovense's getToys API. 


>Copyright (C) 2019  colddenial / openstatic.org

>This program is free software: you can redistribute it and/or modify
>it under the terms of the GNU General Public License as published by
>the Free Software Foundation, either version 3 of the License, or
>(at your option) any later version.

>This program is distributed in the hope that it will be useful,
>but WITHOUT ANY WARRANTY; without even the implied warranty of
>MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
>GNU General Public License for more details.

>You should have received a copy of the GNU General Public License
>along with this program.  If not, see <https://www.gnu.org/licenses/>.
>
