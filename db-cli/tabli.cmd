
@if .%* == . (
	@gradle run
) ELSE (
	@gradle run --args="%*"
)
