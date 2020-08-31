
@if .%* == . (
	@gradle tabul
) ELSE (
	@gradle tabul --args="%*"
)
