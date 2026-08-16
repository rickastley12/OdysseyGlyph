**Title:** I turned my Nothing Phone (3) into a cinema for a meme and accidentally built something useful

**Body:**

> okay so this started because I wanted to watch the Odyssey trailer on the back of my phone. as a meme.
>
> getting it to look decent on the 25×25 matrix took way longer than it should have. turns out naive downsampling looks like garbage — you have to actually average the pixels or faces turn into noise. anyway I went down that rabbit hole and ended up with a pipeline that didn't care what video you gave it, so I kept going.
>
> **what it does:**
> - convert any video from your gallery and play it on the matrix. everything stays on-device
> - live lyrics — reads whatever's playing (any app, not just spotify), pulls synced lyrics from the internet, scrolls them on your glyphs in real time. if the song has no lyrics it falls back to a built-in visualizer so the matrix isn't just sitting there dark
> - widget + quick settings tile — someone on nothing.community pointed out that glyph toys stop after 10 minutes, which kills it mid-album. live lyrics now runs as a background service you can toggle from your home screen or notification shade. stays on as long as you want
>
> **install:** play protect blocks it because of the notification listener permission (needed to read what's playing). 0/66 on virustotal, blanket policy not an actual flag. need adb for now, full guide on the github. appeal submitted.
>
> already had a good discussion going on nothing.community if you want to check that out first.
>
> website + install guide: https://odyssey-glyph.vercel.app/
> github: https://github.com/rickastley12/OdysseyGlyph
