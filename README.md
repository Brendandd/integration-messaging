# Integration Messaging

Contains core messaging functionality including:
<ol>
	<li>Message flow and retry logic</li>
	<li>Message Splitters</li>
	<li>Message Filters</li>
	<li>Message Transformers</li>
</ol>
<br>
For retry I am using the [transactional outbox pattern]([https://microservices.io/patterns/data/transactional-outbox.html) which is a common pattern for microservices.
<br>
<br>
A lot needs to be done as this does not contain complete messaging functionality.
